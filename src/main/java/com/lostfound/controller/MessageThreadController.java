package com.lostfound.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lostfound.common.Result;
import com.lostfound.common.ResultCode;
import com.lostfound.dto.CreateThreadRequest;
import com.lostfound.dto.SendMessageRequest;
import com.lostfound.exception.BusinessException;
import com.lostfound.service.MessageThreadService;
import com.lostfound.vo.MessageVO;
import com.lostfound.vo.ThreadVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/threads")
public class MessageThreadController {

    private final MessageThreadService messageThreadService;

    public MessageThreadController(MessageThreadService messageThreadService) {
        this.messageThreadService = messageThreadService;
    }

    @PostMapping
    public Result<ThreadVO> createThread(@Valid @RequestBody CreateThreadRequest request) {
        return Result.success(messageThreadService.createThread(request, getCurrentUserId()));
    }

    @GetMapping
    public Result<List<ThreadVO>> listThreads() {
        return Result.success(messageThreadService.listThreads(getCurrentUserId()));
    }

    @GetMapping("/{id}")
    public Result<ThreadVO> getThreadDetail(@PathVariable("id") Long id) {
        return Result.success(messageThreadService.getThreadDetail(id, getCurrentUserId()));
    }

    @GetMapping("/{id}/messages")
    public Result<Page<MessageVO>> getMessages(
            @PathVariable("id") Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(messageThreadService.getMessages(id, getCurrentUserId(), page, size));
    }

    @PostMapping("/{id}/messages")
    public Result<MessageVO> sendMessage(@PathVariable("id") Long id, @Valid @RequestBody SendMessageRequest request) {
        return Result.success(messageThreadService.sendMessage(id, request, getCurrentUserId()));
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
