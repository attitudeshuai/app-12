package com.toolshare.controller;

import com.toolshare.config.JwtUtil;
import com.toolshare.entity.Role;
import com.toolshare.entity.User;
import com.toolshare.repository.UserRepository;
import com.toolshare.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminController.class)
@Import({
        com.toolshare.config.SecurityConfig.class,
        com.toolshare.config.JwtAuthenticationFilter.class,
        com.toolshare.config.JwtUtil.class,
        com.fasterxml.jackson.databind.ObjectMapper.class
})
class AdminControllerAuthTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminService adminService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    @DisplayName("未登录用户访问管理员接口 - 应返回403禁止访问")
    void adminApi_NoUser_ShouldForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("普通用户(ROLE_USER)访问管理员接口 - 应返回403无权访问")
    @WithMockUser(username = "regular_user", roles = {"USER"})
    void adminApi_RegularUserRole_ShouldForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("无权访问"));
    }

    @Test
    @DisplayName("普通用户访问管理员工具接口 - 应返回403无权访问")
    @WithMockUser(username = "regular_user", roles = {"USER"})
    void adminApi_RegularUserOnTools_ShouldForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/tools"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("无权访问"));
    }

    @Test
    @DisplayName("普通用户访问借用申请接口 - 应返回403无权访问")
    @WithMockUser(username = "regular_user", roles = {"USER"})
    void adminApi_RegularUserOnBorrowRequests_ShouldForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/borrow-requests"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("无权访问"));
    }

    @Test
    @DisplayName("普通用户访问工具箱接口 - 应返回403无权访问")
    @WithMockUser(username = "regular_user", roles = {"USER"})
    void adminApi_RegularUserOnToolBoxes_ShouldForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/toolboxes"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("无权访问"));
    }

    @Test
    @DisplayName("管理员(ROLE_ADMIN)访问用户接口 - 应成功返回200")
    @WithMockUser(username = "admin_user", roles = {"ADMIN", "USER"})
    void adminApi_AdminUserRoleOnUsers_ShouldSuccess() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("admin_user");
        user.setRole(Role.ADMIN);
        when(userRepository.findByUsername("admin_user")).thenReturn(java.util.Optional.of(user));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("管理员(ROLE_ADMIN)访问工具接口 - 应成功返回200")
    @WithMockUser(username = "admin_user", roles = {"ADMIN", "USER"})
    void adminApi_AdminUserRoleOnTools_ShouldSuccess() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("admin_user");
        user.setRole(Role.ADMIN);
        when(userRepository.findByUsername("admin_user")).thenReturn(java.util.Optional.of(user));

        mockMvc.perform(get("/api/admin/tools"))
                .andExpect(status().isOk());
    }
}
