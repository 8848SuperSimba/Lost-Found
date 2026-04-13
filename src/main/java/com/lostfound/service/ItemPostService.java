package com.lostfound.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lostfound.dto.CreatePostRequest;
import com.lostfound.dto.UpdatePostRequest;
import com.lostfound.enums.ItemCategory;
import com.lostfound.enums.PostStatus;
import com.lostfound.enums.PostType;
import com.lostfound.vo.PostVO;
import java.time.LocalDateTime;

public interface ItemPostService {

    Long createPost(CreatePostRequest request, Long publisherUserId);

    Page<PostVO> listPosts(
            PostType postType,
            ItemCategory category,
            String areaCode,
            PostStatus status,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String keyword,
            Long publisherUserId,
            long page,
            long size);

    PostVO getPostDetail(Long postId, Long currentUserId);

    void updatePost(Long postId, UpdatePostRequest request, Long currentUserId);

    void closePost(Long postId, Long currentUserId, boolean isAdmin, String reason);

    void resolvePost(Long postId, Long currentUserId);
}
