package com.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lostfound.common.ResultCode;
import com.lostfound.entity.Notification;
import com.lostfound.enums.NotificationType;
import com.lostfound.exception.BusinessException;
import com.lostfound.mapper.NotificationMapper;
import com.lostfound.service.NotificationService;
import com.lostfound.vo.NotificationVO;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;

    public NotificationServiceImpl(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    @Override
    public void createNotification(Long userId, NotificationType type, String title, String content, String refType, Long refId) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .content(content)
                .refType(refType)
                .refId(refId)
                .isRead(0)
                .build();
        notificationMapper.insert(notification);
    }

    @Override
    public Page<NotificationVO> listNotifications(Long userId, int page, int size) {
        Page<Notification> notificationPage = notificationMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .orderByDesc(Notification::getCreatedAt));

        List<NotificationVO> records = notificationPage.getRecords().stream().map(this::toVO).toList();
        Page<NotificationVO> resultPage =
                new Page<>(notificationPage.getCurrent(), notificationPage.getSize(), notificationPage.getTotal());
        resultPage.setRecords(records);
        return resultPage;
    }

    @Override
    public void markAsRead(Long notificationId, Long currentUserId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "通知不存在");
        }
        if (!notification.getUserId().equals(currentUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权限操作该通知");
        }
        notificationMapper.updateById(Notification.builder().id(notificationId).isRead(1).build());
    }

    @Override
    public void markAllAsRead(Long userId) {
        Notification update = Notification.builder().isRead(1).build();
        notificationMapper.update(update, new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0));
    }

    @Override
    public Long countUnread(Long userId) {
        return notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>().eq(Notification::getUserId, userId).eq(Notification::getIsRead, 0));
    }

    private NotificationVO toVO(Notification notification) {
        return NotificationVO.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .content(notification.getContent())
                .refType(notification.getRefType())
                .refId(notification.getRefId())
                .isRead(notification.getIsRead() != null && notification.getIsRead() == 1)
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
