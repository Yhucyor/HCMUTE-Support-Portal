package com.school.counseling.module.auth;

import com.school.counseling.module.auth.dto.UserPrincipal;
import com.school.counseling.module.auth.service.DepartmentAccessEvaluator;
import com.school.counseling.module.ticket.dto.TicketAccessAuthInfo;
import com.school.counseling.module.ticket.repository.TicketRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UNIT TEST - MODULE AUTH: Thẩm định phân quyền đơn vị & Chống lỗ hổng IDOR")
class DepartmentAccessEvaluatorTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private ObjectProvider<TicketRepository> ticketRepositoryProvider;

    private DepartmentAccessEvaluator accessEvaluator;

    private UserPrincipal adminPrincipal;
    private UserPrincipal staffDept1;
    private UserPrincipal staffDept2;
    private UserPrincipal studentA;

    @BeforeEach
    void setUp() {
        when(ticketRepositoryProvider.getIfAvailable()).thenReturn(ticketRepository);
        accessEvaluator = new DepartmentAccessEvaluator(ticketRepositoryProvider);

        adminPrincipal = UserPrincipal.builder()
                .id(1L)
                .username("admin")
                .role("ROLE_ADMIN")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .build();

        staffDept1 = UserPrincipal.builder()
                .id(2L)
                .username("staff_pdt")
                .departmentId(10L)
                .role("ROLE_STAFF")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_STAFF")))
                .build();

        staffDept2 = UserPrincipal.builder()
                .id(3L)
                .username("staff_ctsv")
                .departmentId(20L)
                .role("ROLE_STAFF")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_STAFF")))
                .build();

        studentA = UserPrincipal.builder()
                .id(100L)
                .username("student_a")
                .role("ROLE_STUDENT")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(UserPrincipal principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    private TicketAccessAuthInfo createAuthInfo(Long id, Long creatorId, Long departmentId) {
        return new TicketAccessAuthInfo() {
            @Override public Long getId() { return id; }
            @Override public Long getCreatorId() { return creatorId; }
            @Override public Long getDepartmentId() { return departmentId; }
        };
    }

    @Test
    @DisplayName("Admin có toàn quyền truy cập bất kỳ phòng ban nào")
    void testCanAccessDepartment_Admin_HasAccessToAll() {
        authenticate(adminPrincipal);
        assertTrue(accessEvaluator.canAccessDepartment(10L));
        assertTrue(accessEvaluator.canAccessDepartment(20L));
    }

    @Test
    @DisplayName("Cán bộ chỉ truy cập được đúng phòng ban của mình, bị từ chối ở phòng ban khác")
    void testCanAccessDepartment_Staff_RestrictedToOwnDept() {
        authenticate(staffDept1);
        assertTrue(accessEvaluator.canAccessDepartment(10L));
        assertFalse(accessEvaluator.canAccessDepartment(20L));
    }

    @Test
    @DisplayName("Sinh viên không có quyền truy cập vào danh mục quản lý nội bộ phòng ban")
    void testCanAccessDepartment_Student_Denied() {
        authenticate(studentA);
        assertFalse(accessEvaluator.canAccessDepartment(10L));
    }

    @Test
    @DisplayName("Chống IDOR: Sinh viên chỉ xem được Ticket do chính mình tạo ra")
    void testCanAccessTicket_Student_OnlyOwnTicket() {
        authenticate(studentA);

        TicketAccessAuthInfo myTicket = createAuthInfo(501L, 100L, 10L); // creatorId = 100
        TicketAccessAuthInfo othersTicket = createAuthInfo(502L, 999L, 10L); // creatorId = 999

        when(ticketRepository.findAccessAuthInfoById(501L)).thenReturn(Optional.of(myTicket));
        when(ticketRepository.findAccessAuthInfoById(502L)).thenReturn(Optional.of(othersTicket));

        assertTrue(accessEvaluator.canAccessTicket(501L));
        assertFalse(accessEvaluator.canAccessTicket(502L));
    }

    @Test
    @DisplayName("Chống IDOR: Cán bộ chỉ xem được Ticket thuộc phòng ban mình phụ trách")
    void testCanAccessTicket_Staff_RestrictedByDepartment() {
        authenticate(staffDept1); // thuộc dept 10

        TicketAccessAuthInfo ticketDept1 = createAuthInfo(601L, 999L, 10L); // dept 10
        TicketAccessAuthInfo ticketDept2 = createAuthInfo(602L, 999L, 20L); // dept 20

        when(ticketRepository.findAccessAuthInfoById(601L)).thenReturn(Optional.of(ticketDept1));
        when(ticketRepository.findAccessAuthInfoById(602L)).thenReturn(Optional.of(ticketDept2));

        assertTrue(accessEvaluator.canAccessTicket(601L));
        assertFalse(accessEvaluator.canAccessTicket(602L));
    }
}
