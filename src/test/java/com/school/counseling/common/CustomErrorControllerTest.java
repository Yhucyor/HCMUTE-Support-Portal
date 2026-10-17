package com.school.counseling.common;

import com.school.counseling.common.dto.ApiResponse;
import com.school.counseling.common.exception.CustomErrorController;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CustomErrorControllerTest {

    @InjectMocks
    private CustomErrorController errorController;

    @Test
    @DisplayName("Giao diện MVC: Truy cập trang không tồn tại (404) trả về view error với mã 404")
    void testHandleMvc404Error() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 404);
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/trang-khong-ton-tai");
        Model model = new ConcurrentModel();

        Object result = errorController.handleError(request, model);

        assertEquals("error", result);
        assertEquals("404", model.getAttribute("errorCode"));
        assertNotNull(model.getAttribute("errorMessage"));
    }

    @Test
    @DisplayName("REST API: Gọi endpoint lỗi trả về JSON ResponseEntity<ApiResponse<Void>>")
    void testHandleRestApi403Error() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 403);
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/api/v1/admin/secrets");
        Model model = new ConcurrentModel();

        Object result = errorController.handleError(request, model);

        assertTrue(result instanceof ResponseEntity<?>);
        ResponseEntity<?> response = (ResponseEntity<?>) result;
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertTrue(response.getBody() instanceof ApiResponse<?>);
        ApiResponse<?> apiResponse = (ApiResponse<?>) response.getBody();
        assertFalse(apiResponse.isSuccess());
        assertNotNull(apiResponse.getMessage());
    }
}
