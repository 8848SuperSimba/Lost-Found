package com.lostfound.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lostfound.common.Result;
import com.lostfound.common.ResultCode;
import com.lostfound.exception.BusinessException;
import com.lostfound.service.NotificationService;
import com.lostfound.vo.NotificationVO;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public Result<Page<NotificationVO>> listNotifications(
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.success(notificationService.listNotifications(getCurrentUserId(), page, size));
    }

    @GetMapping("/unread-count")
    public Result<Long> countUnread() {
        return Result.success(notificationService.countUnread(getCurrentUserId()));
    }

    @PutMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable("id") Long id) {
        notificationService.markAsRead(id, getCurrentUserId());
        return Result.success();
    }

    @PutMapping("/read-all")
    public Result<Void> markAllAsRead() {
        notificationService.markAllAsRead(getCurrentUserId());
        return Result.success();
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long userId) {
            return userId;
        }
        if (principal instanceof String principalString) {
            try {
                return Long.valueOf(principalString);
            } catch (NumberFormatException ex) {
                throw new BusinessException(ResultCode.UNAUTHORIZED);
            }
        }
        throw new BusinessException(ResultCode.UNAUTHORIZED);
    }
}
