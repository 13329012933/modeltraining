package com.cn.org.modeltraining.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 *  实体类
 * </p>
 *
 * @author gk
 * @since 2025-03-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("model_picture")
public class ModelPicture implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("unit")
    private String unit;

    @TableField("area_name")
    private String areaName;

    @TableField("dev_name")
    private String devName;

    @TableField("vol_type")
    private String volType;

    @TableField("content")
    private String content;

    @TableField("start_time")
    private Date startTime;

    @TableField("end_time")
    private Date endTime;

}
