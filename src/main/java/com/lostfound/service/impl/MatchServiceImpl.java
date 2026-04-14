package com.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lostfound.common.ResultCode;
import com.lostfound.entity.ItemImage;
import com.lostfound.entity.ItemKeyword;
import com.lostfound.entity.ItemPost;
import com.lostfound.entity.MatchResult;
import com.lostfound.entity.Notification;
import com.lostfound.entity.User;
import com.lostfound.enums.NotificationType;
import com.lostfound.enums.PostStatus;
import com.lostfound.enums.PostType;
import com.lostfound.enums.UserRole;
import com.lostfound.exception.BusinessException;
import com.lostfound.mapper.ItemImageMapper;
import com.lostfound.mapper.ItemKeywordMapper;
import com.lostfound.mapper.ItemPostMapper;
import com.lostfound.mapper.MatchResultMapper;
import com.lostfound.mapper.NotificationMapper;
import com.lostfound.mapper.UserMapper;
import com.lostfound.service.MatchService;
import com.lostfound.service.NotificationService;
import com.lostfound.vo.MatchVO;
import com.lostfound.vo.PostVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class MatchServiceImpl implements MatchService {

    private final ItemPostMapper itemPostMapper;
    private final ItemKeywordMapper itemKeywordMapper;
    private final ItemImageMapper itemImageMapper;
    private final MatchResultMapper matchResultMapper;
    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Value("${match.min-score:0.30}")
    private BigDecimal minScore;

    @Value("${match.notify-top-n:3}")
    private int notifyTopN;

    public MatchServiceImpl(
            ItemPostMapper itemPostMapper,
            ItemKeywordMapper itemKeywordMapper,
            ItemImageMapper itemImageMapper,
            MatchResultMapper matchResultMapper,
            NotificationMapper notificationMapper,
            UserMapper userMapper,
            NotificationService notificationService,
            ObjectMapper objectMapper) {
        this.itemPostMapper = itemPostMapper;
        this.itemKeywordMapper = itemKeywordMapper;
        this.itemImageMapper = itemImageMapper;
        this.matchResultMapper = matchResultMapper;
        this.notificationMapper = notificationMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Async
    public void triggerMatchAsync(Long newPostId) {
        performMatching(newPostId, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<MatchVO> reMatchPost(Long postId, Long currentUserId) {
        ItemPost post = itemPostMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        if (!post.getPublisherUserId().equals(currentUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权限重跑匹配");
        }

        matchResultMapper.delete(new LambdaQueryWrapper<MatchResult>().eq(MatchResult::getSrcPostId, postId));
        notificationMapper.update(
                Notification.builder().isRead(1).build(),
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, currentUserId)
                        .eq(Notification::getType, NotificationType.MATCH)
                        .eq(Notification::getRefType, "POST")
                        .eq(Notification::getRefId, postId)
                        .eq(Notification::getIsRead, 0));

        performMatching(postId, false);
        return getMatchResults(postId, currentUserId);
    }

    @Override
    public List<MatchVO> getMatchResults(Long postId, Long currentUserId) {
        ItemPost srcPost = itemPostMapper.selectById(postId);
        if (srcPost == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        User currentUser = userMapper.selectById(currentUserId);
        boolean isAdmin = currentUser != null && currentUser.getRole() == UserRole.ADMIN;
        if (!isAdmin && !srcPost.getPublisherUserId().equals(currentUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权限查看匹配结果");
        }

        List<MatchResult> matchResults = matchResultMapper.selectList(
                new LambdaQueryWrapper<MatchResult>()
                        .eq(MatchResult::getSrcPostId, postId)
                        .orderByDesc(MatchResult::getScore));

        return matchResults.stream().map(this::toMatchVO).toList();
    }

    @Override
    public void triggerAllOpenPostsAsync() {
        List<Long> ids = itemPostMapper
                .selectList(new LambdaQueryWrapper<ItemPost>()
                        .eq(ItemPost::getStatus, PostStatus.OPEN)
                        .select(ItemPost::getId))
                .stream()
                .map(ItemPost::getId)
                .toList();
        for (Long id : ids) {
            triggerMatchAsync(id);
        }
    }

    private void performMatching(Long srcPostId, boolean notify) {
        ItemPost srcPost = itemPostMapper.selectById(srcPostId);
        if (srcPost == null || srcPost.getStatus() != PostStatus.OPEN) {
            return;
        }

        List<ItemPost> candidates = itemPostMapper.selectList(new LambdaQueryWrapper<ItemPost>()
                .eq(ItemPost::getStatus, PostStatus.OPEN)
                .eq(ItemPost::getCategory, srcPost.getCategory())
                .eq(ItemPost::getPostType, oppositeType(srcPost.getPostType()))
                .ne(ItemPost::getId, srcPostId));

        for (ItemPost candidate : candidates) {
            ScoreDetail scoreDetail = calculateScore(srcPost, candidate);
            if (scoreDetail == null || scoreDetail.totalScore.compareTo(minScore) < 0) {
                continue;
            }
            MatchResult matchResult = MatchResult.builder()
                    .srcPostId(srcPostId)
                    .dstPostId(candidate.getId())
                    .score(scoreDetail.totalScore)
                    .reasonJson(buildReasonJson(scoreDetail))
                    .isNotified(0)
                    .build();
            matchResultMapper.insertOrUpdate(matchResult);
        }

        if (!notify) {
            return;
        }

        List<MatchResult> topMatches = matchResultMapper.selectList(new LambdaQueryWrapper<MatchResult>()
                .eq(MatchResult::getSrcPostId, srcPostId)
                .eq(MatchResult::getIsNotified, 0)
                .orderByDesc(MatchResult::getScore)
                .last("LIMIT " + notifyTopN));
        if (topMatches.isEmpty()) {
            return;
        }

        notificationService.createNotification(
                srcPost.getPublisherUserId(),
                NotificationType.MATCH,
                "发现 " + topMatches.size() + " 条匹配信息",
                "您发布的「" + srcPost.getTitle() + "」已匹配到相似信息，请及时查看",
                "POST",
                srcPostId);

        List<Long> ids = topMatches.stream().map(MatchResult::getId).toList();
        matchResultMapper.update(
                MatchResult.builder().isNotified(1).build(),
                new LambdaQueryWrapper<MatchResult>().in(MatchResult::getId, ids));
    }

    private ScoreDetail calculateScore(ItemPost src, ItemPost dst) {
        if (src == null || dst == null) {
            return null;
        }
        if (src.getPostType() == dst.getPostType()) {
            return null;
        }
        if (src.getCategory() != dst.getCategory()) {
            return null;
        }

        double categoryScore = 0.30D;

        List<String> srcKeywords = listKeywords(src.getId());
        List<String> dstKeywords = listKeywords(dst.getId());
        Set<String> srcSet = new HashSet<>(srcKeywords);
        Set<String> dstSet = new HashSet<>(dstKeywords);
        Set<String> intersect = new HashSet<>(srcSet);
        intersect.retainAll(dstSet);
        Set<String> union = new HashSet<>(srcSet);
        union.addAll(dstSet);

        double keywordScore = 0D;
        if (!srcSet.isEmpty() && !dstSet.isEmpty() && !union.isEmpty()) {
            double jaccard = (double) intersect.size() / union.size();
            keywordScore = jaccard * 0.40D;
        }

        double areaScore = 0D;
        if (StringUtils.hasText(src.getAreaCode())
                && StringUtils.hasText(dst.getAreaCode())
                && src.getAreaCode().equals(dst.getAreaCode())) {
            areaScore = 0.20D;
        }

        double timeScore = 0D;
        if (src.getLostFoundTime() != null && dst.getLostFoundTime() != null) {
            long diffHours = Math.abs(Duration.between(src.getLostFoundTime(), dst.getLostFoundTime()).toHours());
            if (diffHours <= 24) {
                timeScore = 0.10D;
            } else if (diffHours <= 72) {
                timeScore = 0.05D;
            }
        }

        double total = categoryScore + keywordScore + areaScore + timeScore;
        return new ScoreDetail(
                BigDecimal.valueOf(total).setScale(4, RoundingMode.HALF_UP),
                categoryScore,
                keywordScore,
                areaScore,
                timeScore,
                new ArrayList<>(intersect));
    }

    private String buildReasonJson(ScoreDetail detail) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("category", scaleDouble(detail.categoryScore));
        map.put("keyword", scaleDouble(detail.keywordScore));
        map.put("area", scaleDouble(detail.areaScore));
        map.put("time", scaleDouble(detail.timeScore));
        map.put("intersectKeywords", detail.intersectKeywords);
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception ex) {
            throw new BusinessException(ResultCode.ERROR, "生成匹配原因失败");
        }
    }

    private MatchVO toMatchVO(MatchResult matchResult) {
        ItemPost dstPost = itemPostMapper.selectById(matchResult.getDstPostId());
        if (dstPost == null) {
            return MatchVO.builder()
                    .matchId(matchResult.getId())
                    .score(matchResult.getScore())
                    .scorePercent(matchResult.getScore().multiply(BigDecimal.valueOf(100)).intValue())
                    .categoryScore(0D)
                    .keywordScore(0D)
                    .areaScore(0D)
                    .timeScore(0D)
                    .matchReasons(Collections.emptyList())
                    .post(null)
                    .build();
        }

        Map<String, Object> reasonMap = parseReasonJson(matchResult.getReasonJson());
        double categoryScore = asDouble(reasonMap.get("category"));
        double keywordScore = asDouble(reasonMap.get("keyword"));
        double areaScore = asDouble(reasonMap.get("area"));
        double timeScore = asDouble(reasonMap.get("time"));
        List<String> intersectKeywords = asStringList(reasonMap.get("intersectKeywords"));

        List<String> reasons = new ArrayList<>();
        if (categoryScore > 0) {
            reasons.add("分类相同");
        }
        if (keywordScore > 0 && !intersectKeywords.isEmpty()) {
            reasons.add("关键词重合：" + String.join(",", intersectKeywords));
        }
        if (areaScore > 0) {
            reasons.add("同一区域");
        }
        if (timeScore >= 0.08D) {
            reasons.add("时间相近");
        }

        User publisher = userMapper.selectById(dstPost.getPublisherUserId());
        ItemImage coverImage = itemImageMapper.selectOne(new LambdaQueryWrapper<ItemImage>()
                .eq(ItemImage::getPostId, dstPost.getId())
                .eq(ItemImage::getSort, 0)
                .last("LIMIT 1"));

        PostVO postVO = PostVO.builder()
                .id(dstPost.getId())
                .postType(dstPost.getPostType())
                .title(dstPost.getTitle())
                .category(dstPost.getCategory())
                .areaText(dstPost.getAreaText())
                .lostFoundTime(dstPost.getLostFoundTime())
                .createdAt(dstPost.getCreatedAt())
                .status(dstPost.getStatus())
                .publisherNickname(publisher == null ? null : publisher.getNickname())
                .publisherAvatar(publisher == null ? null : publisher.getAvatarUrl())
                .coverImageUrl(coverImage == null ? null : coverImage.getUrl())
                .descriptionSummary(buildDescriptionSummary(dstPost.getDescription()))
                .contactInfo(null)
                .build();

        return MatchVO.builder()
                .matchId(matchResult.getId())
                .score(matchResult.getScore())
                .scorePercent(matchResult.getScore().multiply(BigDecimal.valueOf(100)).intValue())
                .categoryScore(categoryScore)
                .keywordScore(keywordScore)
                .areaScore(areaScore)
                .timeScore(timeScore)
                .matchReasons(reasons)
                .post(postVO)
                .build();
    }

    private List<String> listKeywords(Long postId) {
        return itemKeywordMapper
                .selectList(new LambdaQueryWrapper<ItemKeyword>().eq(ItemKeyword::getPostId, postId))
                .stream()
                .map(ItemKeyword::getKeyword)
                .filter(StringUtils::hasText)
                .toList();
    }

    private PostType oppositeType(PostType postType) {
        return postType == PostType.LOST ? PostType.FOUND : PostType.LOST;
    }

    private String buildDescriptionSummary(String description) {
        if (!StringUtils.hasText(description)) {
            return null;
        }
        return description.length() <= 50 ? description : description.substring(0, 50);
    }

    private double scaleDouble(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }

    private Map<String, Object> parseReasonJson(String reasonJson) {
        if (!StringUtils.hasText(reasonJson)) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(reasonJson, new TypeReference<>() {});
        } catch (Exception ex) {
            return new HashMap<>();
        }
    }

    private double asDouble(Object value) {
        if (value == null) {
            return 0D;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ex) {
            return 0D;
        }
    }

    private List<String> asStringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return Collections.emptyList();
        }
        return list.stream().map(String::valueOf).toList();
    }

    private static class ScoreDetail {
        private final BigDecimal totalScore;
        private final double categoryScore;
        private final double keywordScore;
        private final double areaScore;
        private final double timeScore;
        private final List<String> intersectKeywords;

        private ScoreDetail(
                BigDecimal totalScore,
                double categoryScore,
                double keywordScore,
                double areaScore,
                double timeScore,
                List<String> intersectKeywords) {
            this.totalScore = totalScore;
            this.categoryScore = categoryScore;
            this.keywordScore = keywordScore;
            this.areaScore = areaScore;
            this.timeScore = timeScore;
            this.intersectKeywords = intersectKeywords;
        }
    }
}
