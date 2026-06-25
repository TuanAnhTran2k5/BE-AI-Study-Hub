package AiStudyHub.BE.repository;

import AiStudyHub.BE.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepo extends JpaRepository<Notification, Long> {

    // Lấy tất cả notification của user, sắp xếp mới nhất trước
    Page<Notification> findByUserUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // Lấy notification lọc theo isRead
    Page<Notification> findByUserUserIdAndIsReadOrderByCreatedAtDesc(Long userId, Boolean isRead, Pageable pageable);

    // Lấy notification lọc theo type
    Page<Notification> findByUserUserIdAndTypeOrderByCreatedAtDesc(Long userId, String type, Pageable pageable);

    // Lấy notification lọc theo isRead + type
    Page<Notification> findByUserUserIdAndIsReadAndTypeOrderByCreatedAtDesc(Long userId, Boolean isRead, String type, Pageable pageable);

    // Đếm số chưa đọc
    long countByUserUserIdAndIsRead(Long userId, Boolean isRead);

    // Tìm danh sách thông báo theo isRead
    List<Notification> findByUserUserIdAndIsRead(Long userId, Boolean isRead);

    // Xóa tất cả thông báo đã đọc của user
    long deleteByUserUserIdAndIsRead(Long userId, Boolean isRead);

    // Xóa tất cả thông báo thuộc tài liệu bị xóa
    long deleteByDocumentDocumentId(Long documentId);
}
