package com.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lostfound.common.ResultCode;
import com.lostfound.dto.CreatePostRequest;
import com.lostfound.dto.UpdatePostRequest;
import com.lostfound.entity.ItemImage;
import com.lostfound.entity.ItemKeyword;
import com.lostfound.entity.ItemPost;
import com.lostfound.entity.User;
import com.lostfound.enums.ItemCategory;
import com.lostfound.enums.PostStatus;
import com.lostfound.enums.PostType;
import com.lostfound.exception.BusinessException;
import com.lostfound.mapper.ItemImageMapper;
import com.lostfound.mapper.ItemKeywordMapper;
import com.lostfound.mapper.ItemPostMapper;
import com.lostfound.mapper.UserMapper;
import com.lostfound.service.ItemPostService;
import com.lostfound.vo.PostVO;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ItemPostServiceImpl implements ItemPostService {

    private final ItemPostMapper itemPostMapper;
    private final ItemKeywordMapper itemKeywordMapper;
    private final ItemImageMapper itemImageMapper;
    private final UserMapper userMapper;
    private final JdbcTemplate jdbcTemplate;

    public ItemPostServiceImpl(
            ItemPostMapper itemPostMapper,
            ItemKeywordMapper itemKeywordMapper,
            ItemImageMapper itemImageMapper,
            UserMapper userMapper,
            JdbcTemplate jdbcTemplate) {
        this.itemPostMapper = itemPostMapper;
        this.itemKeywordMapper = itemKeywordMapper;
        this.itemImageMapper = itemImageMapper;
        this.userMapper = userMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPost(CreatePostRequest request, Long publisherUserId) {
        validateKeywordSize(request.getKeywords());
        validateImageSize(request.getImageUrls());

        ItemPost itemPost = ItemPost.builder()
                .postType(request.getPostType())
                .title(request.getTitle())
                .category(request.getCategory())
                .description(request.getDescription())
                .lostFoundTime(request.getLostFoundTime())
                .areaCode(request.getAreaCode())
                .areaText(request.getAreaText())
                .locationText(request.getLocationText())
                .contactInfo(request.getContactInfo())
                .reward(request.getReward())
                .status(PostStatus.OPEN)
                .publisherUserId(publisherUserId)
                .build();
        itemPostMapper.insert(itemPost);

        saveKeywords(itemPost.getId(), request.getKeywords());
        saveImages(itemPost.getId(), request.getImageUrls());
        return itemPost.getId();
    }

    @Override
    public Page<PostVO> listPosts(
            PostType postType,
            ItemCategory category,
            String areaCode,
            PostStatus status,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String keyword,
            Long publisherUserId,
            long page,
            long size) {
        LambdaQueryWrapper<ItemPost> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(ItemPost::getCreatedAt);

        if (postType != null) {
            queryWrapper.eq(ItemPost::getPostType, postType);
        }
        if (category != null) {
            queryWrapper.eq(ItemPost::getCategory, category);
        }
        if (StringUtils.hasText(areaCode)) {
            queryWrapper.eq(ItemPost::getAreaCode, areaCode);
        }
        if (status != null) {
            queryWrapper.eq(ItemPost::getStatus, status);
        }
        if (startTime != null) {
            queryWrapper.ge(ItemPost::getLostFoundTime, startTime);
        }
        if (endTime != null) {
            queryWrapper.le(ItemPost::getLostFoundTime, endTime);
        }
        if (StringUtils.hasText(keyword)) {
            queryWrapper.like(ItemPost::getTitle, keyword);
        }
        if (publisherUserId != null) {
            queryWrapper.eq(ItemPost::getPublisherUserId, publisherUserId);
        }

        Page<ItemPost> postPage = itemPostMapper.selectPage(new Page<>(page, size), queryWrapper);
        List<PostVO> records = postPage.getRecords().stream().map(this::toListPostVO).toList();
        Page<PostVO> resultPage = new Page<>(postPage.getCurrent(), postPage.getSize(), postPage.getTotal());
        resultPage.setRecords(records);
        return resultPage;
    }

    @Override
    public PostVO getPostDetail(Long postId, Long currentUserId) {
        ItemPost itemPost = itemPostMapper.selectById(postId);
        if (itemPost == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }

        User publisher = userMapper.selectById(itemPost.getPublisherUserId());
        List<String> keywords = itemKeywordMapper
                .selectList(new LambdaQueryWrapper<ItemKeyword>().eq(ItemKeyword::getPostId, postId))
                .stream()
                .map(ItemKeyword::getKeyword)
                .toList();
        List<String> imageUrls = itemImageMapper
                .selectList(new LambdaQueryWrapper<ItemImage>()
                        .eq(ItemImage::getPostId, postId)
                        .orderByAsc(ItemImage::getSort))
                .stream()
                .map(ItemImage::getUrl)
                .toList();

        return PostVO.builder()
                .id(itemPost.getId())
                .postType(itemPost.getPostType())
                .title(itemPost.getTitle())
                .category(itemPost.getCategory())
                .areaText(itemPost.getAreaText())
                .lostFoundTime(itemPost.getLostFoundTime())
                .createdAt(itemPost.getCreatedAt())
                .status(itemPost.getStatus())
                .publisherNickname(publisher == null ? null : publisher.getNickname())
                .publisherAvatar(publisher == null ? null : publisher.getAvatarUrl())
                .coverImageUrl(imageUrls.isEmpty() ? null : imageUrls.get(0))
                .descriptionSummary(buildDescriptionSummary(itemPost.getDescription()))
                .description(itemPost.getDescription())
                .locationText(itemPost.getLocationText())
                .contactInfo(showContactInfo(itemPost, currentUserId))
                .reward(itemPost.getReward())
                .keywords(keywords)
                .imageUrls(imageUrls)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePost(Long postId, UpdatePostRequest request, Long currentUserId) {
        ItemPost itemPost = itemPostMapper.selectById(postId);
        if (itemPost == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        if (!itemPost.getPublisherUserId().equals(currentUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权限编辑该帖子");
        }
        if (itemPost.getStatus() == PostStatus.RESOLVED || itemPost.getStatus() == PostStatus.CLOSED) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "帖子当前状态不允许编辑");
        }

        validateKeywordSize(request.getKeywords());
        validateImageSize(request.getImageUrls());

        ItemPost updatePost = ItemPost.builder()
                .id(postId)
                .title(request.getTitle())
                .category(request.getCategory())
                .description(request.getDescription())
                .lostFoundTime(request.getLostFoundTime())
                .areaCode(request.getAreaCode())
                .areaText(request.getAreaText())
                .locationText(request.getLocationText())
                .contactInfo(request.getContactInfo())
                .reward(request.getReward())
                .build();
        itemPostMapper.updateById(updatePost);

        if (request.getKeywords() != null) {
            itemKeywordMapper.delete(new LambdaQueryWrapper<ItemKeyword>().eq(ItemKeyword::getPostId, postId));
            saveKeywords(postId, request.getKeywords());
        }
        if (request.getImageUrls() != null) {
            itemImageMapper.delete(new LambdaQueryWrapper<ItemImage>().eq(ItemImage::getPostId, postId));
            saveImages(postId, request.getImageUrls());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closePost(Long postId, Long currentUserId, boolean isAdmin, String reason) {
        ItemPost itemPost = itemPostMapper.selectById(postId);
        if (itemPost == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        if (itemPost.getStatus() == PostStatus.CLOSED) {
            return;
        }
        if (!isAdmin && !itemPost.getPublisherUserId().equals(currentUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权限关闭该帖子");
        }

        itemPostMapper.updateById(ItemPost.builder().id(postId).status(PostStatus.CLOSED).build());
        if (isAdmin) {
            String detail = StringUtils.hasText(reason) ? reason : "管理员关闭帖子";
            jdbcTemplate.update(
                    "INSERT INTO audit_log (admin_user_id, action, target_type, target_id, detail) VALUES (?, ?, ?, ?, ?)",
                    currentUserId,
                    "CLOSE_POST",
                    "POST",
                    postId,
                    detail);
        }
    }

    @Override
    public void resolvePost(Long postId, Long currentUserId) {
        ItemPost itemPost = itemPostMapper.selectById(postId);
        if (itemPost == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        if (!itemPost.getPublisherUserId().equals(currentUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权限标记该帖子");
        }
        if (itemPost.getStatus() == PostStatus.RESOLVED) {
            return;
        }
        itemPostMapper.updateById(ItemPost.builder().id(postId).status(PostStatus.RESOLVED).build());
    }

    private PostVO toListPostVO(ItemPost itemPost) {
        User publisher = userMapper.selectById(itemPost.getPublisherUserId());
        ItemImage coverImage = itemImageMapper.selectOne(new LambdaQueryWrapper<ItemImage>()
                .eq(ItemImage::getPostId, itemPost.getId())
                .eq(ItemImage::getSort, 0)
                .last("LIMIT 1"));
        if (coverImage == null) {
            coverImage = itemImageMapper.selectOne(new LambdaQueryWrapper<ItemImage>()
                    .eq(ItemImage::getPostId, itemPost.getId())
                    .orderByAsc(ItemImage::getSort)
                    .last("LIMIT 1"));
        }

        return PostVO.builder()
                .id(itemPost.getId())
                .postType(itemPost.getPostType())
                .title(itemPost.getTitle())
                .category(itemPost.getCategory())
                .areaText(itemPost.getAreaText())
                .lostFoundTime(itemPost.getLostFoundTime())
                .createdAt(itemPost.getCreatedAt())
                .status(itemPost.getStatus())
                .publisherNickname(publisher == null ? null : publisher.getNickname())
                .publisherAvatar(publisher == null ? null : publisher.getAvatarUrl())
                .coverImageUrl(coverImage == null ? null : coverImage.getUrl())
                .descriptionSummary(buildDescriptionSummary(itemPost.getDescription()))
                .contactInfo(null)
                .build();
    }

    private void saveKeywords(Long postId, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return;
        }
        for (String keyword : keywords) {
            if (!StringUtils.hasText(keyword)) {
                continue;
            }
            itemKeywordMapper.insert(ItemKeyword.builder().postId(postId).keyword(keyword.trim()).build());
        }
    }

    private void saveImages(Long postId, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }
        for (int i = 0; i < imageUrls.size(); i++) {
            String imageUrl = imageUrls.get(i);
            if (!StringUtils.hasText(imageUrl)) {
                continue;
            }
            itemImageMapper.insert(
                    ItemImage.builder().postId(postId).url(imageUrl.trim()).sort(i).build());
        }
    }

    private void validateKeywordSize(List<String> keywords) {
        if (keywords != null && keywords.size() > 10) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "关键词最多10个");
        }
    }

    private void validateImageSize(List<String> imageUrls) {
        if (imageUrls != null && imageUrls.size() > 5) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "图片最多5张");
        }
    }

    private String buildDescriptionSummary(String description) {
        if (!StringUtils.hasText(description)) {
            return null;
        }
        return description.length() <= 50 ? description : description.substring(0, 50);
    }

    private String showContactInfo(ItemPost itemPost, Long currentUserId) {
        if (currentUserId == null) {
            return null;
        }
        return currentUserId.equals(itemPost.getPublisherUserId()) ? itemPost.getContactInfo() : null;
    }
}
