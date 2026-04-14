package com.lostfound.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lostfound.enums.NotificationType;
import com.lostfound.vo.NotificationVO;

public interface NotificationService {

    void createNotification(Long userId, NotificationType type, String title, String content, String refType, Long refId);

    Page<NotificationVO> listNotifications(Long userId, int page, int size);

    void markAsRead(Long notificationId, Long currentUserId);

    void markAllAsRead(Long userId);

    Long countUnread(Long userId);
}
