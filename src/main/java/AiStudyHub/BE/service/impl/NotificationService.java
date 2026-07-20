package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.constraint.UserRole;
import AiStudyHub.BE.dto.Response.DeleteResponse;
import AiStudyHub.BE.dto.Response.NotificationResponse;
import AiStudyHub.BE.entity.Badge;
import AiStudyHub.BE.entity.Document;
import AiStudyHub.BE.entity.Notification;
import AiStudyHub.BE.entity.Ranking;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.exception.GlobalException;
import AiStudyHub.BE.repository.NotificationRepo;
import AiStudyHub.BE.security.SecurityUtils;
import AiStudyHub.BE.service.IEmail;
import AiStudyHub.BE.service.INotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService implements INotification {

    private static final int PAGE_SIZE = 10;

    private final IEmail emailService;
    private final NotificationRepo notificationRepo;

    // =========================================================
    // Internal helpers used by other services
    // =========================================================

    private String loadAndPopulateEmailTemplate(
            String title,
            String userName,
            String documentInfo,
            String actionType,
            String penaltyScoreText,
            String reason,
            String explanation
    ) {
        try {
            ClassPathResource resource = new ClassPathResource("template/report_penalty_template.html");
            String template = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return template.replace("{{title}}", title != null ? title : "")
                    .replace("{{userName}}", userName != null ? userName : "")
                    .replace("{{documentInfo}}", documentInfo != null ? documentInfo : "N/A")
                    .replace("{{actionType}}", actionType != null ? actionType : "N/A")
                    .replace("{{penaltyScore}}", penaltyScoreText != null ? penaltyScoreText : "0 pts")
                    .replace("{{reason}}", reason != null ? reason : "Community guidelines violation")
                    .replace("{{explanation}}", explanation != null ? explanation : "No additional notes provided.");
        } catch (Exception e) {
            log.warn("Could not load template/report_penalty_template.html, falling back to basic HTML format: {}", e.getMessage());
            return String.format(
                    "<h3>%s</h3><p>Dear %s,</p><p>Reported Object: %s<br>Action Taken: %s<br>Reputation Impact: %s<br>Violation Reason: %s<br>Admin Note: %s</p>",
                    title, userName, documentInfo, actionType, penaltyScoreText, reason, explanation
            );
        }
    }

    @Override
    public Notification sendDocumentModerationNotification(
            User owner,
            Document document,
            String reasonName,
            int penaltyScore,
            String actionType,
            String explanation
    ) {
        log.info("Sending moderation notification to document owner {} (ID: {}) for action: {}",
                owner.getEmail(), owner.getUserId(), actionType);

        String subject = "[AI Study Hub] Document Moderation Warning: " + document.getTitle();
        String penaltyText = penaltyScore > 0 ? "-" + penaltyScore + " pts" : "0 pts";
        String htmlContent = loadAndPopulateEmailTemplate(
                "Document Moderation Warning",
                owner.getFullName(),
                document.getTitle(),
                actionType,
                penaltyText,
                reasonName,
                explanation
        );

        emailService.sendEmail(owner.getEmail(), subject, htmlContent);

        String plainTextSummary = String.format(
                "Your document \"%s\" has undergone moderation (%s). Reason: %s. Reputation impact: %s. Details: %s",
                document.getTitle(), actionType, reasonName, penaltyText, explanation != null ? explanation : "None"
        );

        Notification notification = Notification.builder()
                .user(owner)
                .document(document)
                .title(subject)
                .message(plainTextSummary)
                .type("SYSTEM")
                .notificationCase("DOCUMENT_MODERATION")
                .isRead(false)
                .build();
        return notificationRepo.save(notification);
    }

    @Override
    public Notification sendFalseReportPenaltyNotification(
            User reporter,
            Document document,
            int penaltyScore,
            String explanation
    ) {
        log.info("Sending false report penalty notification to reporter {} (ID: {})",
                reporter.getEmail(), reporter.getUserId());

        String subject = "[AI Study Hub] Warning Penalty: False Reporting";
        String penaltyText = "-" + penaltyScore + " pts";
        String htmlContent = loadAndPopulateEmailTemplate(
                "False Report Penalty Notice",
                reporter.getFullName(),
                document != null ? document.getTitle() : "Reported document",
                "Reputation deduction (Spam / False reporting)",
                penaltyText,
                "Inaccurate report determined after Admin moderation review",
                explanation
        );

        emailService.sendEmail(reporter.getEmail(), subject, htmlContent);

        String plainTextSummary = String.format(
                "Your report regarding the document \"%s\" has been rejected after moderation review and determined to be false or spam. Deducted %d reputation points. Note: %s",
                document != null ? document.getTitle() : "", penaltyScore, explanation != null ? explanation : "None"
        );

        Notification notification = Notification.builder()
                .user(reporter)
                .document(document)
                .title(subject)
                .message(plainTextSummary)
                .type("SYSTEM")
                .notificationCase("FALSE_REPORT_PENALTY")
                .isRead(false)
                .build();
        return notificationRepo.save(notification);
    }

    @Override
    public Notification sendAccountBannedNotification(
            User owner,
            Document document,
            String reasonName,
            String explanation
    ) {
        log.info("Sending account banned notification to owner {} (ID: {})",
                owner.getEmail(), owner.getUserId());

        String subject = "[AI Study Hub] Account Ban Notice: Severe Policy Violation";
        String htmlContent = loadBannedEmailTemplate(
                owner.getFullName(),
                document != null ? document.getTitle() : "Personal account",
                reasonName,
                explanation
        );

        emailService.sendEmail(owner.getEmail(), subject, htmlContent);

        String plainTextSummary = String.format(
                "Your account has been locked (BANNED) due to a severe violation of community policies (Document: %s). Reason: %s. Details: %s",
                document != null ? document.getTitle() : "N/A", reasonName, explanation != null ? explanation : "None"
        );

        Notification notification = Notification.builder()
                .user(owner)
                .document(document)
                .title(subject)
                .message(plainTextSummary)
                .type("SYSTEM")
                .notificationCase("ACCOUNT_BANNED")
                .isRead(false)
                .build();
        return notificationRepo.save(notification);
    }

    @Override
    public Notification sendAccountUnbannedNotification(User user) {
        log.info("Sending account unbanned notification to owner {} (ID: {})",
                user.getEmail(), user.getUserId());

        String subject = "[AI Study Hub] Account Restored Notice";
        String htmlContent = loadUnbannedEmailTemplate(
                user.getFullName(),
                "Your account has been unlocked by an administrator. You can now log back in and use the platform normally."
        );

        emailService.sendEmail(user.getEmail(), subject, htmlContent);

        Notification notification = Notification.builder()
                .user(user)
                .title(subject)
                .message("Your account has been unlocked (ACTIVE) by an administrator.")
                .type("SYSTEM")
                .notificationCase("ACCOUNT_UNBANNED")
                .isRead(false)
                .build();
        return notificationRepo.save(notification);
    }

    @Override
    public Notification sendReportApprovedNotification(
            User reporter,
            Document document,
            String explanation
    ) {
        if (reporter == null) return null;

        log.info("Sending report approved notification to reporter {} (ID: {}) for document ID {}",
                reporter.getEmail(), reporter.getUserId(), document != null ? document.getDocumentId() : "N/A");

        String subject = "[AI Study Hub] Thank You For Your Contribution";
        String plainTextSummary = String.format(
                "Thank you for submitting a report regarding the document \"%s\". Our moderation team has reviewed the case and applied appropriate enforcement actions. Your contribution helps keep AI Study Hub safe and trustworthy!",
                document != null ? document.getTitle() : "Reported document"
        );

        Notification notification = Notification.builder()
                .user(reporter)
                .document(document)
                .title(subject)
                .message(plainTextSummary)
                .type("SYSTEM")
                .notificationCase("REPORT_APPROVED")
                .isRead(false)
                .build();
        return notificationRepo.save(notification);
    }

    @Override
    public Notification sendDocumentRestoredNotification(
            User owner,
            Document document,
            String explanation
    ) {
        if (owner == null || document == null) return null;

        log.info("Sending document restored notification to owner {} (ID: {}) for document ID {}",
                owner.getEmail(), owner.getUserId(), document.getDocumentId());

        String subject = "[AI Study Hub] Document Restored: " + document.getTitle();
        String plainTextSummary = String.format(
                "Your document \"%s\" has been reviewed by our moderation team. The related violation reports have been dismissed, and normal visibility of your document has been restored. Note: %s",
                document.getTitle(), explanation != null ? explanation : "No violation found"
        );

        Notification notification = Notification.builder()
                .user(owner)
                .document(document)
                .title(subject)
                .message(plainTextSummary)
                .type("SYSTEM")
                .notificationCase("DOCUMENT_RESTORED")
                .isRead(false)
                .build();
        return notificationRepo.save(notification);
    }

    @Override
    public Notification sendDocumentDownloadNotification(
            User owner,
            User downloader,
            Document document,
            int pointsAwarded
    ) {
        if (owner == null || downloader == null || document == null) return null;
        if (owner.getUserId().equals(downloader.getUserId())) return null;

        log.info("Sending download notification to document owner {} (ID: {}) by downloader {}",
                owner.getEmail(), owner.getUserId(), downloader.getEmail());

        String subject = "[AI Study Hub] Document Downloaded: " + document.getTitle();
        String content = String.format(
                "User %s has downloaded your document \"%s\". You earned +%d reputation points!",
                downloader.getFullName(), document.getTitle(), pointsAwarded
        );

        Notification notification = Notification.builder()
                .user(owner)
                .document(document)
                .title(subject)
                .message(content)
                .type("REWARD")
                .notificationCase("DOCUMENT_DOWNLOAD")
                .isRead(false)
                .build();
        return notificationRepo.save(notification);
    }

    @Override
    public Notification sendDocumentBookmarkNotification(
            User owner,
            User bookmarker,
            Document document,
            int pointsAwarded
    ) {
        if (owner == null || bookmarker == null || document == null) return null;
        if (owner.getUserId().equals(bookmarker.getUserId())) return null;

        log.info("Sending bookmark notification to document owner {} (ID: {}) by bookmarker {}",
                owner.getEmail(), owner.getUserId(), bookmarker.getEmail());

        String subject = "[AI Study Hub] Document Bookmarked: " + document.getTitle();
        String content = String.format(
                "User %s has bookmarked your document \"%s\". You earned +%d reputation points!",
                bookmarker.getFullName(), document.getTitle(), pointsAwarded
        );

        Notification notification = Notification.builder()
                .user(owner)
                .document(document)
                .title(subject)
                .message(content)
                .type("REWARD")
                .notificationCase("DOCUMENT_BOOKMARK")
                .isRead(false)
                .build();
        return notificationRepo.save(notification);
    }

    @Override
    public Notification sendDocumentRatingNotification(
            User owner,
            User rater,
            Document document,
            int ratingValue
    ) {
        if (owner == null || rater == null || document == null) return null;
        if (owner.getUserId().equals(rater.getUserId())) return null;

        log.info("Sending rating notification to document owner {} (ID: {}) by rater {}",
                owner.getEmail(), owner.getUserId(), rater.getEmail());

        String subject = "[AI Study Hub] New Document Rating: " + document.getTitle();
        String content = String.format(
                "User %s rated your document \"%s\" with %d stars!",
                rater.getFullName(), document.getTitle(), ratingValue
        );

        Notification notification = Notification.builder()
                .user(owner)
                .document(document)
                .title(subject)
                .message(content)
                .type("SOCIAL")
                .notificationCase("DOCUMENT_RATING")
                .isRead(false)
                .build();
        return notificationRepo.save(notification);
    }

    @Override
    public Notification sendRankUpNotification(
            User user,
            Ranking newRank
    ) {
        if (user == null || newRank == null) return null;

        log.info("Sending rank up notification to user {} (ID: {}) for new rank {}",
                user.getEmail(), user.getUserId(), newRank.getRankName());

        String subject = "[AI Study Hub] Congratulations! Rank Up: " + newRank.getRankName();
        String content = String.format(
                "Congratulations %s! You have achieved the rank of \"%s\". Keep contributing to earn more rewards and storage limits!",
                user.getFullName(), newRank.getRankName()
        );

        Notification notification = Notification.builder()
                .user(user)
                .title(subject)
                .message(content)
                .type("REWARD")
                .notificationCase("RANK_UP")
                .isRead(false)
                .build();
        return notificationRepo.save(notification);
    }

    @Override
    public Notification sendBadgeAwardedNotification(
            User user,
            Badge badge
    ) {
        if (user == null || badge == null) return null;

        log.info("Sending badge awarded notification to user {} (ID: {}) for badge {}",
                user.getEmail(), user.getUserId(), badge.getBadgeName());

        String subject = "[AI Study Hub] Congratulations! Badge Awarded: " + badge.getBadgeName();
        String content = String.format(
                "Congratulations %s! You have earned the badge \"%s\" (%s). Check out your profile to view your badges!",
                user.getFullName(), badge.getBadgeName(), badge.getDescription() != null ? badge.getDescription() : "New achievement"
        );

        Notification notification = Notification.builder()
                .user(user)
                .title(subject)
                .message(content)
                .type("REWARD")
                .notificationCase("BADGE_AWARDED")
                .isRead(false)
                .build();
        return notificationRepo.save(notification);
    }

    @Override
    public Notification sendRolePromotionNotification(
            User user,
            UserRole newRole
    ) {
        if (user == null || newRole == null) return null;

        log.info("Sending role promotion notification to user {} (ID: {}) for new role {}",
                user.getEmail(), user.getUserId(), newRole.name());

        String roleName = newRole.name().equals("AD") ? "System Administrator" : "Standard User";
        String subject = "[AI Study Hub] Account Role Updated: " + roleName;
        String content = String.format(
                "Dear %s, your account role has been updated to \"%s\" by the system administrator.",
                user.getFullName(), roleName
        );

        // Fallback email content if template is not used
        String htmlContent = String.format(
                "<h3>Account Role Updated</h3><p>Dear %s,</p><p>Your account role has been officially updated to <strong>%s</strong>.</p><p>Please re-login to ensure your new permissions are applied correctly.</p>",
                user.getFullName(), roleName
        );
        
        emailService.sendEmail(user.getEmail(), subject, htmlContent);

        Notification notification = Notification.builder()
                .user(user)
                .title(subject)
                .message(content)
                .type("SYSTEM")
                .notificationCase("ROLE_PROMOTION")
                .isRead(false)
                .build();
        return notificationRepo.save(notification);
    }

    // =========================================================
    // REST API methods
    // =========================================================

    @Override
    public Page<NotificationResponse> getMyNotifications(int page, Boolean isRead, String type) {
        User currentUser = SecurityUtils.getCurrentUser();
        Long userId = currentUser.getUserId();

        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        Page<Notification> notifications;

        boolean hasIsRead = isRead != null;
        boolean hasType = type != null && !type.isBlank();

        if (hasIsRead && hasType) {
            notifications = notificationRepo
                    .findByUserUserIdAndIsReadAndTypeOrderByCreatedAtDesc(userId, isRead, type, pageable);
        } else if (hasIsRead) {
            notifications = notificationRepo
                    .findByUserUserIdAndIsReadOrderByCreatedAtDesc(userId, isRead, pageable);
        } else if (hasType) {
            notifications = notificationRepo
                    .findByUserUserIdAndTypeOrderByCreatedAtDesc(userId, type, pageable);
        } else {
            notifications = notificationRepo
                    .findByUserUserIdOrderByCreatedAtDesc(userId, pageable);
        }

        return notifications.map(this::toResponse);
    }

    @Override
    public long countUnread() {
        User currentUser = SecurityUtils.getCurrentUser();
        return notificationRepo.countByUserUserIdAndIsRead(currentUser.getUserId(), false);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long notificationId) {
        User currentUser = SecurityUtils.getCurrentUser();

        Notification notification = notificationRepo.findById(notificationId)
                .orElseThrow(() -> new GlobalException(404, "Notification not found"));

        if (!notification.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new GlobalException(403, "You do not have permission to update this notification");
        }

        notification.setIsRead(true);
        return toResponse(notificationRepo.save(notification));
    }

    @Override
    @Transactional
    public int markAllAsRead() {
        User currentUser = SecurityUtils.getCurrentUser();
        List<Notification> unreadList = notificationRepo.findByUserUserIdAndIsRead(currentUser.getUserId(), false);
        unreadList.forEach(n -> n.setIsRead(true));
        notificationRepo.saveAll(unreadList);
        return unreadList.size();
    }

    @Override
    @Transactional
    public DeleteResponse deleteNotification(Long notificationId) {
        User currentUser = SecurityUtils.getCurrentUser();

        Notification notification = notificationRepo.findById(notificationId)
                .orElseThrow(() -> new GlobalException(404, "Notification not found"));

        if (!notification.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new GlobalException(403, "You do not have permission to delete this notification");
        }

        notificationRepo.delete(notification);

        return DeleteResponse.builder()
                .success(true)
                .message("Notification deleted successfully")
                .deletedId(notificationId)
                .entityName("Notification")
                .entityIdentifier(notification.getTitle())
                .deletedAt(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public int deleteAllRead() {
        User currentUser = SecurityUtils.getCurrentUser();
        long count = notificationRepo.deleteByUserUserIdAndIsRead(currentUser.getUserId(), true);
        return (int) count;
    }

    // =========================================================
    // Private mapper
    // =========================================================

    private NotificationResponse toResponse(Notification n) {
        NotificationResponse.NotificationResponseBuilder builder = NotificationResponse.builder()
                .notificationId(n.getNotificationId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType())
                .notificationCase(n.getNotificationCase())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt());

        if (n.getDocument() != null) {
            builder.documentId(n.getDocument().getDocumentId())
                   .documentTitle(n.getDocument().getTitle());
        }

        if (n.getReport() != null) {
            builder.reportId(n.getReport().getReportId());
        }

        return builder.build();
    }
    
    private String loadBannedEmailTemplate(
            String userName,
            String documentTitle,
            String reasonName,
            String explanation
    ) {
        try {
            ClassPathResource resource = new ClassPathResource("template/account_banned_template.html");
            String template = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return template.replace("{{userName}}", userName != null ? userName : "")
                    .replace("{{documentTitle}}", documentTitle != null ? documentTitle : "Personal account")
                    .replace("{{reasonName}}", reasonName != null ? reasonName : "Severe Community Guidelines Violation")
                    .replace("{{explanation}}", explanation != null ? explanation : "No additional notes provided.");
        } catch (Exception e) {
            log.warn("Could not load template/account_banned_template.html, falling back to basic HTML format: {}", e.getMessage());
            return String.format(
                    "<h3>Account Banned Notice</h3><p>Dear %s,</p><p>Your account has been locked due to a policy violation.<br>Infringing Content: %s<br>Violation Category: %s<br>Admin Note: %s<br>To appeal, email support@aistudyhub.com.</p>",
                    userName, documentTitle != null ? documentTitle : "Personal account", reasonName, explanation
            );
        }
    }

    private String loadUnbannedEmailTemplate(
            String userName,
            String explanation
    ) {
        try {
            ClassPathResource resource = new ClassPathResource("template/account_unbanned_template.html");
            String template = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return template.replace("{{userName}}", userName != null ? userName : "")
                    .replace("{{explanation}}", explanation != null ? explanation : "Your account restrictions have been lifted following moderation review.");
        } catch (Exception e) {
            log.warn("Could not load template/account_unbanned_template.html, falling back to basic HTML format: {}", e.getMessage());
            return String.format(
                    "<h3>Account Restored Notice</h3><p>Dear %s,</p><p>Your account has been unlocked (ACTIVE) by an administrator.<br>Note: %s<br>Welcome back to AI Study Hub!</p>",
                    userName, explanation != null ? explanation : "Account unlocked."
            );
        }
    }
}
