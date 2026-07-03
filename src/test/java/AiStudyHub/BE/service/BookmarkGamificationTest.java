package AiStudyHub.BE.service;

import AiStudyHub.BE.constraint.ScoreTypeCode;
import AiStudyHub.BE.dto.Response.ScoreContextResponse;
import AiStudyHub.BE.entity.ScoreLog;
import AiStudyHub.BE.entity.ScoreType;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.repository.ScoreLogRepo;
import AiStudyHub.BE.repository.ScoreTypeRepo;
import AiStudyHub.BE.repository.UserRepo;
import AiStudyHub.BE.repository.RankingRepo;
import AiStudyHub.BE.repository.UserRankRepo;
import AiStudyHub.BE.repository.WeeklyScoreRepo;
import AiStudyHub.BE.service.impl.GamificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookmarkGamificationTest {

    @Mock
    private ScoreLogRepo scoreLogRepo;

    @Mock
    private ScoreTypeRepo scoreTypeRepo;

    @Mock
    private UserRepo userRepo;

    @Mock
    private RankingRepo rankingRepo;

    @Mock
    private UserRankRepo userRankRepo;

    @Mock
    private WeeklyScoreRepo weeklyScoreRepo;

    @InjectMocks
    private GamificationService gamificationService;

    private User receiver;
    private ScoreType bookmarkScoreType;

    @BeforeEach
    void setUp() {
        receiver = User.builder()
                .userId(2L)
                .fullName("Owner User")
                .totalScore(10L)
                .build();

        bookmarkScoreType = ScoreType.builder()
                .typeCode(ScoreTypeCode.BOOKMARK.name())
                .typeName("Document Bookmarked")
                .defaultPoint(3)
                .build();
    }

    @Test
    void awardBookmarkScore_firstTime_shouldAwardPoints() {
        // Arrange
        Long actorUserId = 1L;
        String actorFullName = "Actor User";
        Long receiverUserId = 2L;
        Long documentId = 100L;
        String documentTitle = "Spring Boot Guide";
        String visibilityStatus = "PUBLIC";

        when(scoreLogRepo.existsByActorUserIdAndDocumentIdAndScoreTypeTypeCode(
                eq(actorUserId), eq(documentId), eq(ScoreTypeCode.BOOKMARK.name())))
                .thenReturn(false);

        when(scoreTypeRepo.findByTypeCode(eq(ScoreTypeCode.BOOKMARK.name())))
                .thenReturn(Optional.of(bookmarkScoreType));

        when(userRepo.findById(eq(receiverUserId)))
                .thenReturn(Optional.of(receiver));

        when(rankingRepo.findAll()).thenReturn(Collections.emptyList());
        when(weeklyScoreRepo.findByUserAndWeekStart(any(), any())).thenReturn(Optional.empty());

        // Act
        int points = gamificationService.awardBookmarkScore(
                actorUserId, actorFullName, receiverUserId, documentId, documentTitle, visibilityStatus);

        // Assert
        assertEquals(3, points);
        assertEquals(13L, receiver.getTotalScore()); // 10L + 3 points
        verify(userRepo, times(1)).save(receiver); // awardScore saves the user
        verify(scoreLogRepo, times(1)).save(any(ScoreLog.class));
    }

    @Test
    void awardBookmarkScore_sequentialRebookmark_shouldNotAwardPoints() {
        // Arrange
        Long actorUserId = 1L;
        String actorFullName = "Actor User";
        Long receiverUserId = 2L;
        Long documentId = 100L;
        String documentTitle = "Spring Boot Guide";
        String visibilityStatus = "PUBLIC";

        when(scoreLogRepo.existsByActorUserIdAndDocumentIdAndScoreTypeTypeCode(
                eq(actorUserId), eq(documentId), eq(ScoreTypeCode.BOOKMARK.name())))
                .thenReturn(true);

        // Act
        int points = gamificationService.awardBookmarkScore(
                actorUserId, actorFullName, receiverUserId, documentId, documentTitle, visibilityStatus);

        // Assert
        assertEquals(0, points);
        assertEquals(10L, receiver.getTotalScore()); // points unchanged
        verify(userRepo, never()).save(any());
        verify(scoreLogRepo, never()).save(any());
    }
}
