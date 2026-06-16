package com.toolshare.service;

import com.toolshare.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;

    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    public FileStorageService(@Value("${file.upload-dir:uploads}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException ex) {
            throw new RuntimeException("无法创建文件存储目录", ex);
        }
    }

    public String storeFile(MultipartFile file, String subDir) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("不支持的文件类型，仅支持 JPG、PNG、GIF、WEBP 格式的图片");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("文件大小不能超过 5MB");
        }

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = getFileExtension(originalFileName);
        String fileName = UUID.randomUUID().toString() + fileExtension;

        try {
            if (fileName.contains("..")) {
                throw new BadRequestException("文件名包含非法字符: " + originalFileName);
            }

            Path targetLocation = this.fileStorageLocation;
            if (subDir != null && !subDir.isEmpty()) {
                targetLocation = targetLocation.resolve(subDir);
                Files.createDirectories(targetLocation);
            }
            targetLocation = targetLocation.resolve(fileName);

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            if (subDir != null && !subDir.isEmpty()) {
                return "/uploads/" + subDir + "/" + fileName;
            }
            return "/uploads/" + fileName;
        } catch (IOException ex) {
            throw new RuntimeException("无法存储文件 " + fileName, ex);
        }
    }

    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("文件未找到: " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("文件未找到: " + fileName, ex);
        }
    }

    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }
        try {
            String relativePath = fileUrl.replaceFirst("^/uploads/", "");
            Path filePath = this.fileStorageLocation.resolve(relativePath).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
        }
    }

    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex);
        }
        return "";
    }
}
