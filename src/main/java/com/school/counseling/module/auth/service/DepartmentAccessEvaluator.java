package com.school.counseling.module.auth.service;

import com.school.counseling.module.auth.dto.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;

/**
 * Custom Security Bean hỗ trợ SpEL Expression phân quyền theo Khoa/Phòng & chống IDOR.
 * Sử dụng:
 *   - @PreAuthorize("@deptSecurity.canAccessDepartment(#deptId)")
 */
@Slf4j
@Component("deptSecurity")
public class DepartmentAccessEvaluator {

    private final org.springframework.beans.factory.ObjectProvider<com.school.counseling.module.ticket.repository.TicketRepository> ticketRepositoryProvider;

    public DepartmentAccessEvaluator(org.springframework.beans.factory.ObjectProvider<com.school.counseling.module.ticket.repository.TicketRepository> ticketRepositoryProvider) {
        this.ticketRepositoryProvider = ticketRepositoryProvider;
    }

    private static final Set<String> PRIVILEGED_ROLES = Set.of(
            "ROLE_ADMIN",
            "ROLE_SUPER_ADMIN",
            "ROLE_SUPERADMIN"
    );

    /**
     * Lấy Principal của người dùng hiện tại từ Security Context
     */
    public UserPrincipal getCurrentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal;
        }
        return null;
    }

    /**
     * Kiểm tra người dùng có quyền Quản trị cấp cao (Admin / SuperAdmin) không
     */
    public boolean isPrivilegedUser(UserPrincipal user) {
        if (user == null) {
            return false;
        }

        if (user.getRole() != null && PRIVILEGED_ROLES.contains(user.getRole().toUpperCase())) {
            return true;
        }

        if (user.getAuthorities() != null) {
            return user.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(Objects::nonNull)
                    .map(String::toUpperCase)
                    .anyMatch(PRIVILEGED_ROLES::contains);
        }

        return false;
    }

    /**
     * Kiểm tra Cán bộ có thuộc Khoa/Phòng được yêu cầu không (Admin luôn được phép)
     */
    public boolean canAccessDepartment(Long departmentId) {
        if (departmentId == null) {
            log.warn("[Security] canAccessDepartment bị từ chối: departmentId là null");
            return false;
        }

        UserPrincipal user = getCurrentPrincipal();
        if (user == null) {
            return false;
        }

        if (isPrivilegedUser(user)) {
            return true;
        }

        if ("ROLE_STAFF".equalsIgnoreCase(user.getRole())) {
            return Objects.equals(user.getDepartmentId(), departmentId);
        }

        return false;
    }

    /**
     * Backward compatibility method
     */
    public boolean hasAccess(Authentication authentication, Long departmentId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserPrincipal userPrincipal)) {
            return false;
        }

        if (isPrivilegedUser(userPrincipal)) {
            return true;
        }

        return userPrincipal.getDepartmentId() != null && userPrincipal.getDepartmentId().equals(departmentId);
    }

    /**
     * Thẩm định quyền truy cập Ticket chống IDOR
     */
    public boolean canAccessTicket(Long ticketId) {
        if (ticketId == null) {
            log.warn("[Security] canAccessTicket bị từ chối: ticketId là null");
            return false;
        }

        UserPrincipal user = getCurrentPrincipal();
        if (user == null) {
            return false;
        }

        if (isPrivilegedUser(user)) {
            return true;
        }

        var ticketRepository = ticketRepositoryProvider.getIfAvailable();
        if (ticketRepository == null) {
            log.error("[Security] Không thể nạp TicketRepository để thẩm định IDOR!");
            return false;
        }

        var authInfoOpt = ticketRepository.findAccessAuthInfoById(ticketId);
        if (authInfoOpt.isEmpty()) {
            return false;
        }

        var authInfo = authInfoOpt.get();
        if (authInfo.getCreatorId() != null && Objects.equals(authInfo.getCreatorId(), user.getId())) {
            return true;
        }

        if ("ROLE_STAFF".equalsIgnoreCase(user.getRole())) {
            return authInfo.getDepartmentId() != null
                    && Objects.equals(authInfo.getDepartmentId(), user.getDepartmentId());
        }

        return false;
    }
}