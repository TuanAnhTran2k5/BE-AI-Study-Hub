package AiStudyHub.BE;

import AiStudyHub.BE.constraint.*;
import AiStudyHub.BE.dto.Request.BanUserRequest;
import AiStudyHub.BE.dto.Response.*;
import AiStudyHub.BE.entity.*;
import AiStudyHub.BE.exception.GlobalException;
import AiStudyHub.BE.repository.*;
import AiStudyHub.BE.service.impl.DashboardService;
import AiStudyHub.BE.service.impl.UserService;
import AiStudyHub.BE.service.INotification;
import AiStudyHub.BE.config.SystemAdminProperties;
import org.springframework.cache.CacheManager;
import io.qdrant.client.QdrantClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DashboardAndUserAdminTest {

    @Mock
    private UserRepo userRepo;
    @Mock
    private DocumentRepo documentRepo;
    @Mock
    private DownloadRepo downloadRepo;
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private ReportCaseRepo reportCaseRepo;
    @Mock
    private SubjectRepo subjectRepo;
    @Mock
    private RankingRepo rankingRepo;
    @Mock
    private QdrantClient qdrantClient;
    @Mock
    private DataSource dataSource;
    @Mock
    private INotification notificationService;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private SystemAdminProperties systemAdminProperties;

    @InjectMocks
    private DashboardService dashboardService;

    @InjectMocks
    private UserService userService;

    private User adminUser;
    private User regularUser;

    @BeforeEach
    public void setUp() {
        adminUser = User.builder()
                .userId(1L)
                .fullName("Admin User")
                .email("admin@studyhub.com")
                .role(UserRole.AD)
                .status(UserStatus.ACTIVE)
                .build();

        regularUser = User.builder()
                .userId(2L)
                .fullName("Regular User")
                .email("user@studyhub.com")
                .role(UserRole.US)
                .status(UserStatus.ACTIVE)
                .totalScore(150L)
                .build();

        lenient().when(systemAdminProperties.getEmail()).thenReturn("superadmin@studyhub.com");
    }

    @Test
    public void testGetSystemStatistics_Success() {
        // Arrange
        when(userRepo.countByStatus(UserStatus.ACTIVE)).thenReturn(10L);
        when(userRepo.countByStatusAndCreatedAtBetween(any(), any(), any())).thenReturn(5L).thenReturn(10L);

        when(documentRepo.countByUploadStatusAndModerationStatusAndDeletedAtIsNull(any(), any())).thenReturn(20L);
        when(documentRepo.countByUploadStatusAndModerationStatusAndDeletedAtIsNullAndCreatedAtBetween(any(), any(), any(), any())).thenReturn(10L).thenReturn(20L);

        when(downloadRepo.count()).thenReturn(100L);
        when(downloadRepo.countByDownloadedAtBetween(any(), any())).thenReturn(50L).thenReturn(100L);

        when(chatMessageRepository.countBySenderType(SenderType.USER)).thenReturn(200L);
        when(chatMessageRepository.countBySenderTypeAndCreatedAtBetween(any(), any(), any())).thenReturn(100L).thenReturn(200L);

        // Act
        SystemStatisticsResponse stats = dashboardService.getSystemStatistics();

        // Assert
        assertNotNull(stats);
        assertEquals(10L, stats.getTotalActiveUsers().getValue());
        assertEquals("+100.0%", stats.getTotalActiveUsers().getGrowthRate());

        assertEquals(20L, stats.getTotalDocuments().getValue());
        assertEquals("+100.0%", stats.getTotalDocuments().getGrowthRate());

        assertEquals(100L, stats.getTotalDownloads().getValue());
        assertEquals("+100.0%", stats.getTotalDownloads().getGrowthRate());

        assertEquals(200L, stats.getTotalAiQueries().getValue());
        assertEquals("+100.0%", stats.getTotalAiQueries().getGrowthRate());
    }

    @Test
    public void testGetSubjectDistribution_EmptyDocs() {
        when(documentRepo.countByUploadStatusAndModerationStatusAndDeletedAtIsNull(any(), any())).thenReturn(0L);

        List<SubjectDistributionResponse> result = dashboardService.getSubjectDistribution();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetModerationSummary_Success() {
        when(reportCaseRepo.countByCaseStatusIn(any())).thenReturn(3L);
        when(documentRepo.countByReportCountGreaterThanAndModerationStatusAndDeletedAtIsNull(anyInt(), any())).thenReturn(5L);
        when(documentRepo.countByUploadStatusAndDeletedAtIsNull(UploadStatus.PENDING)).thenReturn(2L);
        when(userRepo.countByStatus(UserStatus.BANNED)).thenReturn(1L);
        when(userRepo.countByStatus(UserStatus.PENDING)).thenReturn(4L);

        ModerationSummaryResponse result = dashboardService.getModerationSummary();

        assertNotNull(result);
        assertEquals(3L, result.getPendingReportCasesCount());
        assertEquals(5L, result.getReportedDocumentsCount());
        assertEquals(2L, result.getPendingUploadDocumentsCount());
        assertEquals(1L, result.getTotalBannedUsersCount());
        assertEquals(4L, result.getTotalPendingUsersCount());
    }

    @Test
    public void testBanUser_SelfBan_ThrowsException() {
        // Arrange
        User admin = User.builder().userId(1L).email("admin@studyhub.com").role(UserRole.AD).build();
        try (MockedStatic<AiStudyHub.BE.security.SecurityUtils> mockedSecurity = mockStatic(AiStudyHub.BE.security.SecurityUtils.class)) {
            mockedSecurity.when(AiStudyHub.BE.security.SecurityUtils::getCurrentUser).thenReturn(admin);
            
            when(userRepo.findById(1L)).thenReturn(Optional.of(admin));

            // Act & Assert
            GlobalException exception = assertThrows(GlobalException.class, () -> {
                userService.banUser(1L, "Testing self ban");
            });
            assertEquals(400, exception.getCode());
            assertEquals("Cannot ban yourself", exception.getMessage());
        }
    }

    @Test
    public void testBanUser_BanAdmin_ThrowsException() {
        // Arrange
        User admin = User.builder().userId(1L).email("admin@studyhub.com").role(UserRole.AD).build();
        User targetAdmin = User.builder().userId(3L).email("targetadmin@studyhub.com").role(UserRole.AD).build();
        try (MockedStatic<AiStudyHub.BE.security.SecurityUtils> mockedSecurity = mockStatic(AiStudyHub.BE.security.SecurityUtils.class)) {
            mockedSecurity.when(AiStudyHub.BE.security.SecurityUtils::getCurrentUser).thenReturn(admin);
            
            when(userRepo.findById(1L)).thenReturn(Optional.of(admin));
            when(userRepo.findById(3L)).thenReturn(Optional.of(targetAdmin));

            // Act & Assert
            GlobalException exception = assertThrows(GlobalException.class, () -> {
                userService.banUser(3L, "Testing ban another admin");
            });
            assertEquals(403, exception.getCode());
            assertEquals("Cannot ban another admin account", exception.getMessage());
        }
    }

    @Test
    public void testBanUser_Idempotent_ThrowsException() {
        // Arrange
        User admin = User.builder().userId(1L).email("admin@studyhub.com").role(UserRole.AD).build();
        User targetBanned = User.builder().userId(2L).email("target@studyhub.com").role(UserRole.US).status(UserStatus.BANNED).build();
        try (MockedStatic<AiStudyHub.BE.security.SecurityUtils> mockedSecurity = mockStatic(AiStudyHub.BE.security.SecurityUtils.class)) {
            mockedSecurity.when(AiStudyHub.BE.security.SecurityUtils::getCurrentUser).thenReturn(admin);
            
            when(userRepo.findById(1L)).thenReturn(Optional.of(admin));
            when(userRepo.findById(2L)).thenReturn(Optional.of(targetBanned));

            // Act & Assert
            GlobalException exception = assertThrows(GlobalException.class, () -> {
                userService.banUser(2L, "Testing already banned");
            });
            assertEquals(409, exception.getCode());
            assertEquals("User is already banned", exception.getMessage());
        }
    }

    @Test
    public void testBanUser_Success_SendsNotification() {
        // Arrange
        User admin = User.builder().userId(1L).role(UserRole.AD).build();
        User target = User.builder().userId(2L).role(UserRole.US).status(UserStatus.ACTIVE).email("target@studyhub.com").build();
        
        try (MockedStatic<AiStudyHub.BE.security.SecurityUtils> mockedSecurity = mockStatic(AiStudyHub.BE.security.SecurityUtils.class)) {
            mockedSecurity.when(AiStudyHub.BE.security.SecurityUtils::getCurrentUser).thenReturn(admin);
            
            when(userRepo.findById(1L)).thenReturn(Optional.of(admin));
            when(userRepo.findById(2L)).thenReturn(Optional.of(target));
            when(documentRepo.countActiveDocumentsGroupByOwnerIds(any())).thenReturn(new ArrayList<>());
            when(downloadRepo.countDownloadsReceivedGroupByOwnerIds(any())).thenReturn(new ArrayList<>());

            // Act
            AdminUserResponse response = userService.banUser(2L, "Violation of rules");

            // Assert
            assertNotNull(response);
            assertEquals(UserStatus.BANNED, response.getStatus());
            verify(notificationService, times(1)).sendAccountBannedNotification(eq(target), eq(null), anyString(), eq("Violation of rules"));
            verify(userRepo, times(1)).save(target);
        }
    }

    @Test
    public void testUnbanUser_Success_SendsNotification() {
        // Arrange
        User admin = User.builder().userId(1L).role(UserRole.AD).build();
        User target = User.builder().userId(2L).role(UserRole.US).status(UserStatus.BANNED).email("target@studyhub.com").build();
        
        try (MockedStatic<AiStudyHub.BE.security.SecurityUtils> mockedSecurity = mockStatic(AiStudyHub.BE.security.SecurityUtils.class)) {
            mockedSecurity.when(AiStudyHub.BE.security.SecurityUtils::getCurrentUser).thenReturn(admin);
            
            when(userRepo.findById(1L)).thenReturn(Optional.of(admin));
            when(userRepo.findById(2L)).thenReturn(Optional.of(target));
            when(documentRepo.countActiveDocumentsGroupByOwnerIds(any())).thenReturn(new ArrayList<>());
            when(downloadRepo.countDownloadsReceivedGroupByOwnerIds(any())).thenReturn(new ArrayList<>());

            // Act
            AdminUserResponse response = userService.unbanUser(2L);

            // Assert
            assertNotNull(response);
            assertEquals(UserStatus.ACTIVE, response.getStatus());
            verify(notificationService, times(1)).sendAccountUnbannedNotification(eq(target));
            verify(userRepo, times(1)).save(target);
        }
    }

    @Test
    public void testRefreshDashboardCaches_IdleSkip() {
        // Act
        dashboardService.refreshDashboardCaches();
        
        // Assert
        verifyNoInteractions(chatMessageRepository);
    }

    @Test
    public void testRefreshDashboardCaches_Active_WarmsCache() {
        // Arrange
        when(userRepo.countByStatus(UserStatus.ACTIVE)).thenReturn(10L);
        
        org.springframework.cache.Cache mockCache = mock(org.springframework.cache.Cache.class);
        when(cacheManager.getCache(anyString())).thenReturn(mockCache);

        // Act
        dashboardService.getSystemStatistics();
        dashboardService.refreshDashboardCaches();

        // Assert
        verify(mockCache, times(3)).put(eq(org.springframework.cache.interceptor.SimpleKey.EMPTY), any());
    }
}
