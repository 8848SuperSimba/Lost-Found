package com.lostfound.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lostfound.common.Result;
import com.lostfound.common.ResultCode;
import com.lostfound.dto.CreatePostRequest;
import com.lostfound.dto.UpdatePostRequest;
import com.lostfound.enums.ItemCategory;
import com.lostfound.enums.PostStatus;
import com.lostfound.enums.PostType;
import com.lostfound.exception.BusinessException;
import com.lostfound.service.ItemPostService;
import com.lostfound.service.MatchService;
import com.lostfound.vo.PostVO;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class PostController {

    private final ItemPostService itemPostService;

    @Autowired
    private MatchService matchService;

    public PostController(ItemPostService itemPostService) {
        this.itemPostService = itemPostService;
    }

    @PostMapping("/api/posts")
    public Result<Long> createPost(@Valid @RequestBody CreatePostRequest request) {
        Long currentUserId = getRequiredCurrentUserId();
        Long newPostId = itemPostService.createPost(request, currentUserId);
        triggerMatchAsync(newPostId);
        return Result.success(newPostId);
    }

    @GetMapping("/api/posts")
    public Result<Page<PostVO>> listPosts(
            @RequestParam(required = false) PostType postType,
            @RequestParam(required = false) ItemCategory category,
            @RequestParam(required = false) String areaCode,
            @RequestParam(required = false) PostStatus status,
            @RequestParam(required = false) LocalDateTime startTime,
            @RequestParam(required = false) LocalDateTime endTime,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long publisherUserId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.success(itemPostService.listPosts(
                postType, category, areaCode, status, startTime, endTime, keyword, publisherUserId, page, size));
    }

    @GetMapping("/api/posts/{id}")
    public Result<PostVO> getPostDetail(@PathVariable("id") Long id) {
        return Result.success(itemPostService.getPostDetail(id, getOptionalCurrentUserId()));
    }

    @PutMapping("/api/posts/{id}")
    public Result<Void> updatePost(@PathVariable("id") Long id, @Valid @RequestBody UpdatePostRequest request) {
        itemPostService.updatePost(id, request, getRequiredCurrentUserId());
        return Result.success();
    }

    @DeleteMapping("/api/posts/{id}")
    public Result<Void> closePost(@PathVariable("id") Long id) {
        Long currentUserId = getRequiredCurrentUserId();
        boolean isAdmin = isCurrentUserAdmin();
        itemPostService.closePost(id, currentUserId, isAdmin, null);
        return Result.success();
    }

    @PutMapping("/api/posts/{id}/resolve")
    public Result<Void> resolvePost(@PathVariable("id") Long id) {
        itemPostService.resolvePost(id, getRequiredCurrentUserId());
        return Result.success();
    }

    @GetMapping("/api/admin/posts")
    public Result<Page<PostVO>> adminListPosts(
            @RequestParam(required = false) PostType postType,
            @RequestParam(required = false) ItemCategory category,
            @RequestParam(required = false) String areaCode,
            @RequestParam(required = false) PostStatus status,
            @RequestParam(required = false) LocalDateTime startTime,
            @RequestParam(required = false) LocalDateTime endTime,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long publisherUserId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.success(itemPostService.listPosts(
                postType, category, areaCode, status, startTime, endTime, keyword, publisherUserId, page, size));
    }

    @DeleteMapping("/api/admin/posts/{id}")
    public Result<Void> adminClosePost(@PathVariable("id") Long id, @RequestBody(required = false) Map<String, String> request) {
        String reason = request == null ? null : request.get("reason");
        if (request != null && request.containsKey("reason") && !StringUtils.hasText(reason)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "reason 不能为空");
        }
        itemPostService.closePost(id, getRequiredCurrentUserId(), true, reason);
        return Result.success();
    }

    @Async
    public void triggerMatchAsync(Long postId) {
        matchService.triggerMatchAsync(postId);
    }

    private Long getRequiredCurrentUserId() {
        Long userId = getOptionalCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }

    private Long getOptionalCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long userId) {
            return userId;
        }
        if (principal instanceof String principalString) {
            try {
                return Long.valueOf(principalString);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    private boolean isCurrentUserAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
