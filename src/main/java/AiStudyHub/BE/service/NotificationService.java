package AiStudyHub.BE.service;

import AiStudyHub.BE.entity.Document;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.service.impl.IEmail;
import AiStudyHub.BE.service.impl.INotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService implements INotification {

    private final IEmail emailService;

    @Override
    public void sendDocumentModerationNotification(
            User owner,
            Document document,
            String reasonName,
            int penaltyScore,
            String actionType,
            String explanation
    ) {
        log.info("Sending moderation notification to document owner {} (ID: {}) for action: {}", owner.getEmail(), owner.getUserId(), actionType);
        
        String subject = "[AI Study Hub] Cảnh báo kiểm duyệt tài liệu: " + document.getTitle();
        String content = String.format(
                "Xin chào %s,\n\n" +
                "Tài liệu của bạn \"%s\" đã bị xử lý kiểm duyệt.\n" +
                "Lý do: %s\n" +
                "Biện pháp xử lý: %s\n" +
                "Điểm uy tín bị trừ: %d\n" +
                "Chi tiết/Giải trình: %s\n\n" +
                "Trân trọng,\nBan quản trị AI Study Hub.",
                owner.getFullName(),
                document.getTitle(),
                reasonName,
                actionType,
                penaltyScore,
                explanation != null ? explanation : "Không có"
        );

        emailService.sendEmail(owner.getEmail(), subject, content);
    }

    @Override
    public void sendFalseReportPenaltyNotification(
            User reporter,
            Document document,
            int penaltyScore,
            String explanation
    ) {
        log.info("Sending false report penalty notification to reporter {} (ID: {})", reporter.getEmail(), reporter.getUserId());

        String subject = "[AI Study Hub] Phạt cảnh cáo: Gửi báo cáo sai sự thật";
        String content = String.format(
                "Xin chào %s,\n\n" +
                "Báo cáo của bạn về tài liệu \"%s\" đã bị bác bỏ sau khi kiểm duyệt.\n" +
                "Hành vi này được xác định là gửi báo cáo sai sự thật hoặc spam phá hoại hệ thống.\n" +
                "Biện pháp xử lý: Trừ %d điểm uy tín vào tài khoản của bạn.\n" +
                "Lý do từ chối: %s\n\n" +
                "Vui lòng lưu ý, nếu điểm uy tín của bạn tiếp tục xuống mức âm, tính năng báo cáo của bạn sẽ bị khoá vĩnh viễn.\n\n" +
                "Trân trọng,\nBan quản trị AI Study Hub.",
                reporter.getFullName(),
                document.getTitle(),
                penaltyScore,
                explanation != null ? explanation : "Không có"
        );

        emailService.sendEmail(reporter.getEmail(), subject, content);
    }
}
