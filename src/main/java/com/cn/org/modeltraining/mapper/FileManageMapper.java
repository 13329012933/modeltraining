package com.cn.org.modeltraining.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cn.org.modeltraining.entity.FileManage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author gk
 * @since 2025-03-25
 */
@Mapper
public interface FileManageMapper extends BaseMapper<FileManage> {

}
