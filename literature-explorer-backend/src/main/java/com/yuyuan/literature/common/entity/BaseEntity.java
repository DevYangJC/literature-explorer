package com.yuyuan.literature.common.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 基础实体类
 *
 * @author Literature Assistant
 * @since 1.0.0
 */
@Data
@Schema(description = "基础实体")
public class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Schema(description = "主键ID")
    private Long id;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    /**
     * 创建者ID
     */
    @Schema(description = "创建者ID")
    private Long createBy;

    /**
     * 更新者ID
     */
    @Schema(description = "更新者ID")
    private Long updateBy;

    /**
     * 删除标记：0-正常，1-删除
     */
    @Schema(description = "删除标记：0-正常，1-删除")
    private Integer deleteFlag;

    /**
     * 版本号（乐观锁）
     */
    @Schema(description = "版本号")
    private Integer version;
}
