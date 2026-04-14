package com.lostfound.controller;

import com.lostfound.common.Result;
import com.lostfound.common.ResultCode;
import com.lostfound.exception.BusinessException;
import com.lostfound.service.MatchService;
import com.lostfound.vo.MatchVO;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping("/api/posts/{id}/matches")
    public Result<List<MatchVO>> getMatchResults(@PathVariable("id") Long id) {
        return Result.success(matchService.getMatchResults(id, getCurrentUserId()));
    }

    @PostMapping("/api/posts/{id}/rematch")
    public Result<List<MatchVO>> reMatch(@PathVariable("id") Long id) {
        return Result.success(matchService.reMatchPost(id, getCurrentUserId()));
    }

    @PostMapping("/api/admin/match/trigger")
    public Result<String> triggerAllMatches() {
        matchService.triggerAllOpenPostsAsync();
        return Result.success("匹配任务已全部提交，正在后台执行");
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
