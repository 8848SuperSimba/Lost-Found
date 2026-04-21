package com.lostfound.service.impl;

import com.lostfound.enums.ItemCategory;
import com.lostfound.service.StatsService;
import com.lostfound.vo.AreaStatVO;
import com.lostfound.vo.CategoryStatVO;
import com.lostfound.vo.OverviewVO;
import com.lostfound.vo.TrendStatVO;
import com.lostfound.vo.UserStatVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class StatsServiceImpl implements StatsService {

    private static final String OVERVIEW_KEY = "stats:overview";
    private static final String USER_STATS_KEY = "stats:users";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final JdbcTemplate jdbcTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    public StatsServiceImpl(JdbcTemplate jdbcTemplate, RedisTemplate<String, Object> redisTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public OverviewVO getOverview() {
        Object cached = redisTemplate.opsForValue().get(OVERVIEW_KEY);
        if (cached instanceof OverviewVO overviewVO) {
            return overviewVO;
        }

        Long lostCount = queryCount("SELECT COUNT(*) FROM item_post WHERE post_type = 'LOST'");
        Long foundCount = queryCount("SELECT COUNT(*) FROM item_post WHERE post_type = 'FOUND'");
        Long resolvedCount = queryCount("SELECT COUNT(*) FROM item_post WHERE status = 'RESOLVED'");
        Long todayCount = queryCount("SELECT COUNT(*) FROM item_post WHERE DATE(created_at) = CURDATE()");
        Long openCount = queryCount("SELECT COUNT(*) FROM item_post WHERE status = 'OPEN'");

        String resolvedRate = "0.00%";
        if (lostCount != null && lostCount > 0) {
            BigDecimal rate = BigDecimal.valueOf(resolvedCount == null ? 0L : resolvedCount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(lostCount), 2, RoundingMode.HALF_UP);
            resolvedRate = rate.toPlainString() + "%";
        }

        OverviewVO result = OverviewVO.builder()
                .lostCount(defaultCount(lostCount))
                .foundCount(defaultCount(foundCount))
                .resolvedCount(defaultCount(resolvedCount))
                .resolvedRate(resolvedRate)
                .todayCount(defaultCount(todayCount))
                .openCount(defaultCount(openCount))
                .build();
        redisTemplate.opsForValue().set(OVERVIEW_KEY, result, 5, TimeUnit.MINUTES);
        return result;
    }

    @Override
    public List<CategoryStatVO> getCategoryStats(String postType, Integer days) {
        String key = "stats:category:" + normalizeKeyPart(postType) + ":" + normalizeKeyPart(days);
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof List<?> list) {
            @SuppressWarnings("unchecked")
            List<CategoryStatVO> categoryStats = (List<CategoryStatVO>) list;
            return categoryStats;
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT category, COUNT(*) AS count ")
                .append("FROM item_post ")
                .append("WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (StringUtils.hasText(postType)) {
            sql.append("AND post_type = ? ");
            params.add(postType.trim().toUpperCase(Locale.ROOT));
        }
        if (days != null) {
            sql.append("AND created_at >= DATE_SUB(NOW(), INTERVAL ? DAY) ");
            params.add(days);
        }
        sql.append("GROUP BY category ORDER BY count DESC");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), params.toArray());
        long total = rows.stream().mapToLong(row -> ((Number) row.get("count")).longValue()).sum();

        List<CategoryStatVO> result = rows.stream().map(row -> {
            String categoryValue = String.valueOf(row.get("category"));
            long count = ((Number) row.get("count")).longValue();
            BigDecimal percent = total == 0
                    ? BigDecimal.ZERO
                    : BigDecimal.valueOf(count)
                            .multiply(BigDecimal.valueOf(100))
                            .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
            return CategoryStatVO.builder()
                    .categoryValue(categoryValue)
                    .categoryDesc(resolveCategoryDesc(categoryValue))
                    .count(count)
                    .percentage(percent.toPlainString() + "%")
                    .build();
        }).toList();

        redisTemplate.opsForValue().set(key, result, 5, TimeUnit.MINUTES);
        return result;
    }

    @Override
    public List<AreaStatVO> getAreaStats(String postType, Integer days) {
        String key = "stats:area:" + normalizeKeyPart(postType) + ":" + normalizeKeyPart(days);
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof List<?> list) {
            @SuppressWarnings("unchecked")
            List<AreaStatVO> areaStats = (List<AreaStatVO>) list;
            return areaStats;
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT area_code, area_text, COUNT(*) AS count ")
                .append("FROM item_post ")
                .append("WHERE area_code IS NOT NULL AND area_code != '' ");
        List<Object> params = new ArrayList<>();
        if (StringUtils.hasText(postType)) {
            sql.append("AND post_type = ? ");
            params.add(postType.trim().toUpperCase(Locale.ROOT));
        }
        if (days != null) {
            sql.append("AND created_at >= DATE_SUB(NOW(), INTERVAL ? DAY) ");
            params.add(days);
        }
        sql.append("GROUP BY area_code, area_text ORDER BY count DESC");

        List<AreaStatVO> result = jdbcTemplate.query(
                sql.toString(),
                (rs, rowNum) -> AreaStatVO.builder()
                        .areaCode(rs.getString("area_code"))
                        .areaText(rs.getString("area_text"))
                        .count(rs.getLong("count"))
                        .build(),
                params.toArray());

        redisTemplate.opsForValue().set(key, result, 5, TimeUnit.MINUTES);
        return result;
    }

    @Override
    public List<TrendStatVO> getTrend(String postType, Integer days) {
        int queryDays = days == null ? 30 : days;
        String key = "stats:trend:" + normalizeKeyPart(postType) + ":" + queryDays;
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof List<?> list) {
            @SuppressWarnings("unchecked")
            List<TrendStatVO> trendStats = (List<TrendStatVO>) list;
            return trendStats;
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DATE(created_at) AS date, COUNT(*) AS count ")
                .append("FROM item_post ")
                .append("WHERE created_at >= DATE_SUB(NOW(), INTERVAL ? DAY) ");
        List<Object> params = new ArrayList<>();
        params.add(queryDays);
        if (StringUtils.hasText(postType)) {
            sql.append("AND post_type = ? ");
            params.add(postType.trim().toUpperCase(Locale.ROOT));
        }
        sql.append("GROUP BY DATE(created_at) ORDER BY date ASC");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), params.toArray());
        Map<String, Long> countByDate = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object dateObj = row.get("date");
            String dateText;
            if (dateObj instanceof Date sqlDate) {
                dateText = sqlDate.toLocalDate().format(DATE_FORMATTER);
            } else {
                dateText = String.valueOf(dateObj);
            }
            countByDate.put(dateText, ((Number) row.get("count")).longValue());
        }

        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(queryDays);
        List<TrendStatVO> result = new ArrayList<>();
        for (LocalDate cursor = start; !cursor.isAfter(end); cursor = cursor.plusDays(1)) {
            String date = cursor.format(DATE_FORMATTER);
            result.add(TrendStatVO.builder()
                    .date(date)
                    .count(countByDate.getOrDefault(date, 0L))
                    .build());
        }

        redisTemplate.opsForValue().set(key, result, 5, TimeUnit.MINUTES);
        return result;
    }

    @Override
    public UserStatVO getUserStats() {
        Object cached = redisTemplate.opsForValue().get(USER_STATS_KEY);
        if (cached instanceof UserStatVO userStatVO) {
            return userStatVO;
        }

        Long totalCount = queryCount("SELECT COUNT(*) FROM user");
        Long todayCount = queryCount("SELECT COUNT(*) FROM user WHERE DATE(created_at) = CURDATE()");
        Long bannedCount = queryCount("SELECT COUNT(*) FROM user WHERE status = 'BANNED'");
        Long activeCount = queryCount("SELECT COUNT(*) FROM user WHERE last_login_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)");

        UserStatVO result = UserStatVO.builder()
                .totalCount(defaultCount(totalCount))
                .todayCount(defaultCount(todayCount))
                .bannedCount(defaultCount(bannedCount))
                .activeCount(defaultCount(activeCount))
                .build();
        redisTemplate.opsForValue().set(USER_STATS_KEY, result, 5, TimeUnit.MINUTES);
        return result;
    }

    private Long queryCount(String sql) {
        return jdbcTemplate.queryForObject(sql, Long.class);
    }

    private Long defaultCount(Long value) {
        return value == null ? 0L : value;
    }

    private String normalizeKeyPart(Object value) {
        return value == null ? "ALL" : String.valueOf(value);
    }

    private String resolveCategoryDesc(String categoryValue) {
        if (!StringUtils.hasText(categoryValue)) {
            return "";
        }
        try {
            ItemCategory category = ItemCategory.valueOf(categoryValue);
            return category.getDesc();
        } catch (IllegalArgumentException ex) {
            return categoryValue;
        }
    }
}
