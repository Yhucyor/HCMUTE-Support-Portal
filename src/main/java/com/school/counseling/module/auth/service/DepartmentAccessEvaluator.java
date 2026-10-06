package com.school.counseling.module.auth.service;

import com.school.counseling.module.auth.dto.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("deptSecurity")
public class DepartmentAccessEvaluator {

    /**
     * Checks if the authenticated user has access to a specific department.
     * Admins have universal access; Staff only access their own department.
     */
    public boolean hasAccess(Authentication authentication, Long departmentId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserPrincipal userPrincipal)) {
            return false;
        }

        // ROLE_ADMIN has access to all departments
        if (userPrincipal.getRole() != null && userPrincipal.getRole().equals("ROLE_ADMIN")) {
            return true;
        }

        // Staff can only access their assigned department
        return userPrincipal.getDepartmentId() != null && userPrincipal.getDepartmentId().equals(departmentId);
    }
}
