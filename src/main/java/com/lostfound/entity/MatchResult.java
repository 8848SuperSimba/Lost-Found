package com.lostfound.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("match_result")
public class MatchResult {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("src_post_id")
    private Long srcPostId;

    @TableField("dst_post_id")
    private Long dstPostId;

    private BigDecimal score;

    @TableField("reason_json")
    private String reasonJson;

    @TableField("is_notified")
    private Integer isNotified;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
