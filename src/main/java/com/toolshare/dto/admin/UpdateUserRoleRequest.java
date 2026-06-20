package com.toolshare.dto.admin;

import com.toolshare.entity.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserRoleRequest {
    @NotNull(message = "角色不能为空")
    private Role role;
}
