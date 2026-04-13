-- 校园失物招领智能匹配系统 - MySQL Schema (Revised)
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS lost_found
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;   -- unicode_ci 比 general_ci 排序更准确

USE lost_found;

-- ----------------------------
-- user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `username`      VARCHAR(64)     NULL,
  `phone`         VARCHAR(32)     NULL,
  `email`         VARCHAR(128)    NULL,
  `wx_openid`     VARCHAR(64)     NULL COMMENT '微信公众号 openid',
  `password_hash` VARCHAR(255)    NULL COMMENT '微信用户可无密码',
  `nickname`      VARCHAR(64)     NULL,
  `avatar_url`    VARCHAR(512)    NULL,
  `role`          ENUM('USER','ADMIN') NOT NULL DEFAULT 'USER',
  `status`        ENUM('ACTIVE','BANNED') NOT NULL DEFAULT 'ACTIVE',
  `last_login_at` DATETIME        NULL,
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`),
  UNIQUE KEY `uk_user_phone` (`phone`),
  UNIQUE KEY `uk_user_email` (`email`),
  UNIQUE KEY `uk_user_wx_openid` (`wx_openid`),
  KEY `idx_user_role_status` (`role`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- item_post
-- ----------------------------
DROP TABLE IF EXISTS `item_post`;
CREATE TABLE `item_post` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `post_type`         ENUM('LOST','FOUND') NOT NULL,
  `title`             VARCHAR(128)    NOT NULL,
  `category`          VARCHAR(32)     NOT NULL COMMENT '物品分类：证件/数码/钥匙/衣物/其他',
  `description`       TEXT            NULL,
  `lost_found_time`   DATETIME        NULL COMMENT '遗失或拾获时间',
  `area_code`         VARCHAR(32)     NULL COMMENT '校区区域编码',
  `area_text`         VARCHAR(64)     NULL COMMENT '区域名称，如图书馆/食堂A',
  `location_text`     VARCHAR(255)    NULL COMMENT '详细位置描述',
  `contact_info`      VARCHAR(128)    NULL COMMENT '发布者留下的联系方式（可选）',
  `reward`            VARCHAR(64)     NULL COMMENT '悬赏描述（可选）',
  `status`            ENUM('OPEN','MATCHED','RESOLVED','CLOSED') NOT NULL DEFAULT 'OPEN',
  `publisher_user_id` BIGINT UNSIGNED NOT NULL,
  `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  FULLTEXT KEY `ft_item_post_search` (`title`, `description`) WITH PARSER ngram,  -- 中文分词
  KEY `idx_item_post_type_status_created` (`post_type`, `status`, `created_at`),
  KEY `idx_item_post_category` (`category`),
  KEY `idx_item_post_area` (`area_code`),
  KEY `idx_item_post_time` (`lost_found_time`),
  KEY `idx_item_post_publisher` (`publisher_user_id`, `created_at`),
  CONSTRAINT `fk_item_post_publisher`
    FOREIGN KEY (`publisher_user_id`) REFERENCES `user` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- item_keyword（关键词拆分表，支持精确标签匹配）
-- ----------------------------
DROP TABLE IF EXISTS `item_keyword`;
CREATE TABLE `item_keyword` (
  `id`      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `post_id` BIGINT UNSIGNED NOT NULL,
  `keyword` VARCHAR(32)     NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_keyword_post` (`post_id`),
  KEY `idx_keyword_word` (`keyword`),
  CONSTRAINT `fk_keyword_post`
    FOREIGN KEY (`post_id`) REFERENCES `item_post` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- item_image
-- ----------------------------
DROP TABLE IF EXISTS `item_image`;
CREATE TABLE `item_image` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `post_id`    BIGINT UNSIGNED NOT NULL,
  `url`        VARCHAR(1024)   NOT NULL,
  `sort`       INT             NOT NULL DEFAULT 0,
  `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_item_image_post_sort` (`post_id`, `sort`),
  CONSTRAINT `fk_item_image_post`
    FOREIGN KEY (`post_id`) REFERENCES `item_post` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- message_thread
-- ----------------------------
DROP TABLE IF EXISTS `message_thread`;
CREATE TABLE `message_thread` (
  `id`                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `lost_post_id`       BIGINT UNSIGNED NOT NULL,
  `found_post_id`      BIGINT UNSIGNED NOT NULL,
  `initiator_user_id`  BIGINT UNSIGNED NOT NULL COMMENT '发起联系的一方',
  `receiver_user_id`   BIGINT UNSIGNED NOT NULL COMMENT '被联系的一方',
  `status`             ENUM('ACTIVE','CLOSED') NOT NULL DEFAULT 'ACTIVE',
  `created_at`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_thread_pair` (`lost_post_id`, `found_post_id`),
  KEY `idx_thread_initiator` (`initiator_user_id`, `updated_at`),
  KEY `idx_thread_receiver` (`receiver_user_id`, `updated_at`),
  CONSTRAINT `fk_thread_lost_post`
    FOREIGN KEY (`lost_post_id`) REFERENCES `item_post` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_thread_found_post`
    FOREIGN KEY (`found_post_id`) REFERENCES `item_post` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_thread_initiator`
    FOREIGN KEY (`initiator_user_id`) REFERENCES `user` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_thread_receiver`
    FOREIGN KEY (`receiver_user_id`) REFERENCES `user` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- message
-- ----------------------------
DROP TABLE IF EXISTS `message`;
CREATE TABLE `message` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `thread_id`      BIGINT UNSIGNED NOT NULL,
  `sender_user_id` BIGINT UNSIGNED NOT NULL,
  `content`        TEXT            NOT NULL,
  `is_read`        TINYINT(1)      NOT NULL DEFAULT 0,
  `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_message_thread_time` (`thread_id`, `created_at`),
  KEY `idx_message_sender` (`sender_user_id`, `created_at`),
  CONSTRAINT `fk_message_thread`
    FOREIGN KEY (`thread_id`) REFERENCES `message_thread` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_message_sender`
    FOREIGN KEY (`sender_user_id`) REFERENCES `user` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- match_result
-- ----------------------------
DROP TABLE IF EXISTS `match_result`;
CREATE TABLE `match_result` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `src_post_id`  BIGINT UNSIGNED NOT NULL COMMENT '触发匹配的帖子',
  `dst_post_id`  BIGINT UNSIGNED NOT NULL COMMENT '被匹配到的帖子',
  `score`        DECIMAL(6,4)    NOT NULL COMMENT '综合相似度 0~1',
  `reason_json`  JSON            NULL COMMENT '各维度得分明细',
  `is_notified`  TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否已推送给用户',
  `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_match_pair` (`src_post_id`, `dst_post_id`),
  KEY `idx_match_src_score` (`src_post_id`, `score` DESC),
  CONSTRAINT `fk_match_src`
    FOREIGN KEY (`src_post_id`) REFERENCES `item_post` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_match_dst`
    FOREIGN KEY (`dst_post_id`) REFERENCES `item_post` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- notification（站内通知）
-- ----------------------------
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '接收通知的用户',
  `type`        ENUM('MATCH','MESSAGE','SYSTEM') NOT NULL,
  `title`       VARCHAR(128)    NOT NULL,
  `content`     VARCHAR(512)    NULL,
  `ref_type`    VARCHAR(32)     NULL COMMENT '关联对象类型：POST/THREAD',
  `ref_id`      BIGINT UNSIGNED NULL COMMENT '关联对象 ID',
  `is_read`     TINYINT(1)      NOT NULL DEFAULT 0,
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_notif_user_read` (`user_id`, `is_read`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- audit_log
-- ----------------------------
DROP TABLE IF EXISTS `audit_log`;
CREATE TABLE `audit_log` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `admin_user_id` BIGINT UNSIGNED NOT NULL,
  `action`        VARCHAR(64)     NOT NULL COMMENT '如：BAN_USER / DELETE_POST',
  `target_type`   VARCHAR(64)     NOT NULL,
  `target_id`     BIGINT UNSIGNED NULL,
  `detail`        VARCHAR(1024)   NULL,
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_audit_admin_time` (`admin_user_id`, `created_at`),
  KEY `idx_audit_target` (`target_type`, `target_id`),
  CONSTRAINT `fk_audit_admin`
    FOREIGN KEY (`admin_user_id`) REFERENCES `user` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;