package com.lostfound.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lostfound.enums.ThreadStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("message_thread")
public class MessageThread {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("lost_post_id")
    private Long lostPostId;

    @TableField("found_post_id")
    private Long foundPostId;

    @TableField("initiator_user_id")
    private Long initiatorUserId;

    @TableField("receiver_user_id")
    private Long receiverUserId;

    private ThreadStatus status;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
