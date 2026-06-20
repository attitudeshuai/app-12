package com.toolshare.service;

import com.toolshare.dto.PageResponse;
import com.toolshare.dto.helppost.*;
import com.toolshare.entity.*;
import com.toolshare.exception.BadRequestException;
import com.toolshare.exception.ResourceNotFoundException;
import com.toolshare.repository.HelpPostRepository;
import com.toolshare.repository.HelpResponseRepository;
import com.toolshare.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class HelpBoardService {

    private final HelpPostRepository helpPostRepository;
    private final HelpResponseRepository helpResponseRepository;
    private final UserRepository userRepository;

    public HelpBoardService(HelpPostRepository helpPostRepository,
                            HelpResponseRepository helpResponseRepository,
                            UserRepository userRepository) {
        this.helpPostRepository = helpPostRepository;
        this.helpResponseRepository = helpResponseRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public HelpPostResponse createHelpPost(CreateHelpPostRequest request, Long posterId) {
        HelpPost helpPost = new HelpPost();
        helpPost.setPosterId(posterId);
        helpPost.setTitle(request.getTitle());
        helpPost.setContent(request.getContent());
        helpPost.setCategory(request.getCategory());
        helpPost.setLocation(request.getLocation());
        helpPost.setDeadline(request.getDeadline());
        helpPost.setStatus(HelpPostStatus.OPEN);

        HelpPost savedPost = helpPostRepository.save(helpPost);
        return toHelpPostResponse(savedPost);
    }

    @Transactional
    public HelpPostResponse updateHelpPost(Long postId, UpdateHelpPostRequest request, Long userId) {
        HelpPost helpPost = helpPostRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("求助帖不存在"));

        if (!helpPost.getPosterId().equals(userId)) {
            throw new BadRequestException("只有发布者可以修改此求助帖");
        }

        if (request.getTitle() != null) {
            helpPost.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            helpPost.setContent(request.getContent());
        }
        if (request.getCategory() != null) {
            helpPost.setCategory(request.getCategory());
        }
        if (request.getLocation() != null) {
            helpPost.setLocation(request.getLocation());
        }
        if (request.getDeadline() != null) {
            helpPost.setDeadline(request.getDeadline());
        }
        if (request.getStatus() != null) {
            helpPost.setStatus(request.getStatus());
        }

        HelpPost savedPost = helpPostRepository.save(helpPost);
        return toHelpPostResponse(savedPost);
    }

    @Transactional
    public void deleteHelpPost(Long postId, Long userId) {
        HelpPost helpPost = helpPostRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("求助帖不存在"));

        if (!helpPost.getPosterId().equals(userId)) {
            throw new BadRequestException("只有发布者可以删除此求助帖");
        }

        helpPostRepository.delete(helpPost);
    }

    public HelpPostResponse getHelpPostById(Long postId) {
        HelpPost helpPost = helpPostRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("求助帖不存在"));
        return toHelpPostResponse(helpPost);
    }

    public PageResponse<HelpPostResponse> getAllHelpPosts(int page, int size, String category, List<HelpPostStatus> statuses) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<HelpPost> postPage;

        if (statuses != null && !statuses.isEmpty() && category != null && !category.isEmpty()) {
            postPage = helpPostRepository.findByStatusInAndCategory(statuses, category, pageable);
        } else if (statuses != null && !statuses.isEmpty()) {
            postPage = helpPostRepository.findByStatusIn(statuses, pageable);
        } else if (category != null && !category.isEmpty()) {
            postPage = helpPostRepository.findByCategory(category, pageable);
        } else {
            postPage = helpPostRepository.findAll(pageable);
        }

        Page<HelpPostResponse> responsePage = postPage.map(this::toHelpPostResponse);
        return PageResponse.from(responsePage);
    }

    public PageResponse<HelpPostResponse> getMyHelpPosts(Long posterId, int page, int size, HelpPostStatus status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<HelpPost> postPage;

        if (status != null) {
            postPage = helpPostRepository.findByPosterIdAndStatus(posterId, status, pageable);
        } else {
            postPage = helpPostRepository.findByPosterId(posterId, pageable);
        }

        Page<HelpPostResponse> responsePage = postPage.map(this::toHelpPostResponse);
        return PageResponse.from(responsePage);
    }

    public PageResponse<HelpPostResponse> getAcceptedHelpPosts(Long acceptedResponderId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<HelpPost> postPage = helpPostRepository.findByAcceptedResponderId(acceptedResponderId, pageable);
        Page<HelpPostResponse> responsePage = postPage.map(this::toHelpPostResponse);
        return PageResponse.from(responsePage);
    }

    @Transactional
    public HelpResponseResponse createHelpResponse(Long postId, CreateHelpResponseRequest request, Long responderId) {
        HelpPost helpPost = helpPostRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("求助帖不存在"));

        if (helpPost.getStatus() != HelpPostStatus.OPEN) {
            throw new BadRequestException("该求助帖当前状态不接受响应");
        }

        if (helpPost.getPosterId().equals(responderId)) {
            throw new BadRequestException("不能响应自己发布的求助帖");
        }

        if (helpResponseRepository.existsByHelpPostIdAndResponderId(postId, responderId)) {
            throw new BadRequestException("您已经响应过此求助帖");
        }

        HelpResponse helpResponse = new HelpResponse();
        helpResponse.setHelpPostId(postId);
        helpResponse.setResponderId(responderId);
        helpResponse.setMessage(request.getMessage());
        helpResponse.setContactInfo(request.getContactInfo());
        helpResponse.setAccepted(false);

        HelpResponse savedResponse = helpResponseRepository.save(helpResponse);
        return toHelpResponseResponse(savedResponse);
    }

    @Transactional
    public HelpResponseResponse acceptHelpResponse(Long postId, Long responseId, Long userId) {
        HelpPost helpPost = helpPostRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("求助帖不存在"));

        if (!helpPost.getPosterId().equals(userId)) {
            throw new BadRequestException("只有发布者可以接受响应");
        }

        if (helpPost.getStatus() != HelpPostStatus.OPEN) {
            throw new BadRequestException("该求助帖当前状态不能接受响应");
        }

        HelpResponse helpResponse = helpResponseRepository.findById(responseId)
                .orElseThrow(() -> new ResourceNotFoundException("响应不存在"));

        if (!helpResponse.getHelpPostId().equals(postId)) {
            throw new BadRequestException("该响应不属于此求助帖");
        }

        helpResponse.setAccepted(true);
        helpResponseRepository.save(helpResponse);

        helpPost.setStatus(HelpPostStatus.ASSIGNED);
        helpPost.setAcceptedResponderId(helpResponse.getResponderId());
        helpPostRepository.save(helpPost);

        return toHelpResponseResponse(helpResponse);
    }

    @Transactional
    public HelpPostResponse completeHelpPost(Long postId, Long userId) {
        HelpPost helpPost = helpPostRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("求助帖不存在"));

        if (!helpPost.getPosterId().equals(userId) && !helpPost.getAcceptedResponderId().equals(userId)) {
            throw new BadRequestException("只有发布者或被接受的响应者可以标记完成");
        }

        if (helpPost.getStatus() != HelpPostStatus.ASSIGNED) {
            throw new BadRequestException("只有已分配状态的求助帖可以标记完成");
        }

        helpPost.setStatus(HelpPostStatus.COMPLETED);
        HelpPost savedPost = helpPostRepository.save(helpPost);
        return toHelpPostResponse(savedPost);
    }

    @Transactional
    public HelpPostResponse cancelHelpPost(Long postId, Long userId) {
        HelpPost helpPost = helpPostRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("求助帖不存在"));

        if (!helpPost.getPosterId().equals(userId)) {
            throw new BadRequestException("只有发布者可以取消求助帖");
        }

        if (helpPost.getStatus() == HelpPostStatus.COMPLETED) {
            throw new BadRequestException("已完成的求助帖不能取消");
        }

        helpPost.setStatus(HelpPostStatus.CANCELLED);
        HelpPost savedPost = helpPostRepository.save(helpPost);
        return toHelpPostResponse(savedPost);
    }

    public List<HelpResponseResponse> getHelpResponsesByPostId(Long postId) {
        HelpPost helpPost = helpPostRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("求助帖不存在"));

        List<HelpResponse> responses = helpResponseRepository.findByHelpPostIdOrderByCreatedAtDesc(postId);
        List<HelpResponseResponse> result = new ArrayList<>();
        for (HelpResponse response : responses) {
            result.add(toHelpResponseResponse(response));
        }
        return result;
    }

    public PageResponse<HelpResponseResponse> getMyHelpResponses(Long responderId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<HelpResponse> responsePage = helpResponseRepository.findByResponderId(responderId, pageable);
        Page<HelpResponseResponse> resultPage = responsePage.map(this::toHelpResponseResponse);
        return PageResponse.from(resultPage);
    }

    private HelpPostResponse toHelpPostResponse(HelpPost helpPost) {
        HelpPostResponse response = new HelpPostResponse();
        response.setId(helpPost.getId());
        response.setPosterId(helpPost.getPosterId());
        response.setTitle(helpPost.getTitle());
        response.setContent(helpPost.getContent());
        response.setCategory(helpPost.getCategory());
        response.setStatus(helpPost.getStatus());
        response.setLocation(helpPost.getLocation());
        response.setDeadline(helpPost.getDeadline());
        response.setAcceptedResponderId(helpPost.getAcceptedResponderId());
        response.setCreatedAt(helpPost.getCreatedAt());
        response.setUpdatedAt(helpPost.getUpdatedAt());

        Long count = helpPostRepository.countResponsesByHelpPostId(helpPost.getId());
        response.setResponseCount(count != null ? count.intValue() : 0);

        userRepository.findById(helpPost.getPosterId()).ifPresent(user ->
                response.setPosterName(user.getUsername())
        );

        if (helpPost.getAcceptedResponderId() != null) {
            userRepository.findById(helpPost.getAcceptedResponderId()).ifPresent(user ->
                    response.setAcceptedResponderName(user.getUsername())
            );
        }

        return response;
    }

    private HelpResponseResponse toHelpResponseResponse(HelpResponse helpResponse) {
        HelpResponseResponse response = new HelpResponseResponse();
        response.setId(helpResponse.getId());
        response.setHelpPostId(helpResponse.getHelpPostId());
        response.setResponderId(helpResponse.getResponderId());
        response.setMessage(helpResponse.getMessage());
        response.setContactInfo(helpResponse.getContactInfo());
        response.setAccepted(helpResponse.isAccepted());
        response.setCreatedAt(helpResponse.getCreatedAt());

        userRepository.findById(helpResponse.getResponderId()).ifPresent(user ->
                response.setResponderName(user.getUsername())
        );

        helpPostRepository.findById(helpResponse.getHelpPostId()).ifPresent(post ->
                response.setHelpPostTitle(post.getTitle())
        );

        return response;
    }
}
