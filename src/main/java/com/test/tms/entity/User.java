package com.test.tms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户，对应表 {@code tms_user}。
 * <p>用于多用户场景；密码仅存哈希，不明文存储。</p>
 */
@Data
@TableName("tms_user")
public class User {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 登录名，全局唯一 */
    private String username;

    /** 密码哈希（可空，视认证方案而定） */
    @TableField("password_hash")
    private String passwordHash;

    /** 创建时间 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 创建人用户 id（可空） */
    @TableField("created_by")
    private Long createdBy;

    /** 最后更新时间 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    /** 最后更新人用户 id（可空） */
    @TableField("updated_by")
    private Long updatedBy;
}
