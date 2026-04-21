package com.lostfound.controller;

import com.lostfound.common.Result;
import com.lostfound.common.ResultCode;
import com.lostfound.exception.BusinessException;
import com.lostfound.service.StatsService;
import com.lostfound.vo.AreaStatVO;
import com.lostfound.vo.CategoryStatVO;
import com.lostfound.vo.OverviewVO;
import com.lostfound.vo.TrendStatVO;
import com.lostfound.vo.UserStatVO;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/api/stats/overview")
    public Result<OverviewVO> getOverview() {
        return Result.success(statsService.getOverview());
    }

    @GetMapping("/api/stats/category")
    public Result<List<CategoryStatVO>> getCategoryStats(
            @RequestParam(required = false) String postType, @RequestParam(required = false) Integer days) {
        return Result.success(statsService.getCategoryStats(postType, days));
    }

    @GetMapping("/api/stats/area")
    public Result<List<AreaStatVO>> getAreaStats(
            @RequestParam(required = false) String postType, @RequestParam(required = false) Integer days) {
        return Result.success(statsService.getAreaStats(postType, days));
    }

    @GetMapping("/api/stats/trend")
    public Result<List<TrendStatVO>> getTrend(
            @RequestParam(required = false) String postType, @RequestParam(defaultValue = "30") Integer days) {
        return Result.success(statsService.getTrend(postType, days));
    }

    @GetMapping("/api/admin/stats/users")
    public Result<UserStatVO> getUserStats() {
        getCurrentUserId();
        return Result.success(statsService.getUserStats());
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
