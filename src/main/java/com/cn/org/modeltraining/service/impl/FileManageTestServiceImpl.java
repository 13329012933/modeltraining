package com.cn.org.modeltraining.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cn.org.modeltraining.entity.FileManage;
import com.cn.org.modeltraining.entity.FileManageTest;
import com.cn.org.modeltraining.mapper.FileManageMapper;
import com.cn.org.modeltraining.mapper.FileManageTestMapper;
import com.cn.org.modeltraining.service.IFileManageService;
import com.cn.org.modeltraining.service.IFileManageTestService;
import org.springframework.stereotype.Service;


/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author gk
 * @since 2025-03-25
 */
@Service
public class FileManageTestServiceImpl extends ServiceImpl<FileManageTestMapper, FileManageTest> implements IFileManageTestService {

}
