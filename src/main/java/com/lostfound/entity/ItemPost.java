package com.lostfound.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lostfound.enums.ItemCategory;
import com.lostfound.enums.PostStatus;
import com.lostfound.enums.PostType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("item_post")
public class ItemPost {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("post_type")
    private PostType postType;

    private String title;
    private ItemCategory category;
    private String description;

    @TableField(value = "lost_found_time", updateStrategy = FieldStrategy.NOT_NULL)
    private LocalDateTime lostFoundTime;

    @TableField(value = "area_code", updateStrategy = FieldStrategy.NOT_NULL)
    private String areaCode;

    @TableField(value = "area_text", updateStrategy = FieldStrategy.NOT_NULL)
    private String areaText;

    @TableField(value = "location_text", updateStrategy = FieldStrategy.NOT_NULL)
    private String locationText;

    @TableField(value = "contact_info", updateStrategy = FieldStrategy.NOT_NULL)
    private String contactInfo;

    @TableField(updateStrategy = FieldStrategy.NOT_NULL)
    private String reward;

    private PostStatus status;

    @TableField("publisher_user_id")
    private Long publisherUserId;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
