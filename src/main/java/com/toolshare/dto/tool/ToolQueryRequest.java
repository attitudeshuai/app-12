package com.toolshare.dto.tool;

import com.toolshare.entity.ToolStatus;
import lombok.Data;

import java.util.List;

@Data
public class ToolQueryRequest {
    private String keyword;
    private String category;
    private ToolStatus status;
    private Long boxId;
    private List<Long> boxIds;
    private String sortBy;
    private String sortDir;
    private int page;
    private int size;
}
