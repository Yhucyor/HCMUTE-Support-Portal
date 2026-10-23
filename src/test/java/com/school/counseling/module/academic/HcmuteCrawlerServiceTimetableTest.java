package com.school.counseling.module.academic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.counseling.module.academic.dto.HcmuteSyncResponse;
import com.school.counseling.module.academic.entity.CourseSchedule;
import com.school.counseling.module.academic.repository.*;
import com.school.counseling.module.academic.service.HcmuteCrawlerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Academic Timetable & Schedule Integration Tests")
class HcmuteCrawlerServiceTimetableTest {

    @Mock
    private StudentGradeRepository gradeRepository;
    @Mock
    private CourseScheduleRepository scheduleRepository;
    @Mock
    private CurriculumSubjectRepository curriculumRepository;
    @Mock
    private SyncHistoryRepository syncHistoryRepository;
    @Mock
    private StudentProfileRepository profileRepository;

    private HcmuteCrawlerService crawlerService;

    @BeforeEach
    void setUp() {
        crawlerService = new HcmuteCrawlerService(
                gradeRepository,
                scheduleRepository,
                curriculumRepository,
                syncHistoryRepository,
                profileRepository,
                new ObjectMapper()
        );
    }

    @Test
    @DisplayName("calculateTimesFromShift - Tính toán đúng khung giờ theo tiết chuẩn HCMUTE")
    void testCalculateTimesFromShift() {
        // Morning shifts
        String[] t1_3 = HcmuteCrawlerService.calculateTimesFromShift("Tiết 1 - 3");
        assertEquals("07:30", t1_3[0]);
        assertEquals("09:45", t1_3[1]);

        String[] t4_6 = HcmuteCrawlerService.calculateTimesFromShift("Tiết 4 - 6");
        assertEquals("10:00", t4_6[0]);
        assertEquals("12:15", t4_6[1]);

        // Afternoon shifts
        String[] t7_9 = HcmuteCrawlerService.calculateTimesFromShift("Tiết 7 - 9");
        assertEquals("13:00", t7_9[0]);
        assertEquals("15:15", t7_9[1]);

        String[] t10_12 = HcmuteCrawlerService.calculateTimesFromShift("Tiết 10 - 12");
        assertEquals("15:30", t10_12[0]);
        assertEquals("17:45", t10_12[1]);

        // HCMUTE Portal real API formats
        String[] portal1 = HcmuteCrawlerService.calculateTimesFromShift("1 (07g30)");
        assertEquals("07:30", portal1[0]);
        assertEquals("09:45", portal1[1]);

        String[] portal7 = HcmuteCrawlerService.calculateTimesFromShift("7 (12g45)");
        assertEquals("12:45", portal7[0]);
        assertEquals("15:00", portal7[1]);

        String[] portal10 = HcmuteCrawlerService.calculateTimesFromShift("10 (15g15)");
        assertEquals("15:15", portal10[0]);
        assertEquals("17:30", portal10[1]);

        // Null or blank fallback
        String[] fallback = HcmuteCrawlerService.calculateTimesFromShift(null);
        assertEquals("07:30", fallback[0]);
        assertEquals("09:45", fallback[1]);
    }

    @Test
    @DisplayName("syncDemoData - Sinh thời khóa biểu demo đầy đủ startTime, endTime, groupCode, credits")
    @SuppressWarnings("unchecked")
    void testSyncDemoData_IncludesTimetableAttributes() {
        String studentId = "21110123";

        HcmuteSyncResponse response = crawlerService.syncDemoData(studentId);

        assertTrue(response.isSuccess());
        assertEquals("DEMO_FALLBACK", response.getSyncMode());

        ArgumentCaptor<List<CourseSchedule>> captor = ArgumentCaptor.forClass(List.class);
        verify(scheduleRepository).saveAll(captor.capture());

        List<CourseSchedule> savedSchedules = captor.getValue();
        assertNotNull(savedSchedules);
        assertFalse(savedSchedules.isEmpty());

        for (CourseSchedule s : savedSchedules) {
            assertEquals(studentId, s.getStudentId());
            assertNotNull(s.getCourseCode(), "Mã môn học không được rỗng");
            assertNotNull(s.getCourseName(), "Tên môn học không được rỗng");
            assertNotNull(s.getDayOfWeek(), "Thứ học không được rỗng");
            assertNotNull(s.getShift(), "Ca học không được rỗng");
            assertNotNull(s.getStartTime(), "Giờ bắt đầu không được rỗng: " + s.getCourseName());
            assertNotNull(s.getEndTime(), "Giờ kết thúc không được rỗng: " + s.getCourseName());
            assertNotNull(s.getGroupCode(), "Nhóm học không được rỗng");
            assertNotNull(s.getCredits(), "Số tín chỉ không được rỗng");
            assertTrue(s.getCredits() > 0, "Số tín chỉ phải lớn hơn 0");
        }
    }
}
