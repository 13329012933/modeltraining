package com.cn.org.modeltraining.controller;


import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cn.org.modeltraining.commom.Result;
import com.cn.org.modeltraining.entity.FileManage;
import com.cn.org.modeltraining.service.IDemoService;
import com.cn.org.modeltraining.service.IFileManageService;
import com.cn.org.modeltraining.utils.JsonToFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author gk
 * @since 2025-03-25
 */
@RestController
@RequestMapping("/demo/training")
public class DemoController {

    @Value("${file.localFileRootWindows}")
    private String fileDir;
    @Value("${file.picPathRootWindows}")
    private String allFilePath;
    @Autowired
    private IDemoService service;
    @Autowired
    private IFileManageService iFileManageService;
    @RequestMapping("/getPicCoordinate")
    public Result<?> getPicCoordinate(@RequestBody JSONObject jsonObject){
        String fileName = jsonObject.getString("fileName");
        jsonObject.remove("fileName");
        String jsonString = jsonObject.toJSONString();
        String jsonPath = JsonToFile.toJsonFile(jsonString, fileDir, fileName);
        return Result.ok("JSON文件生成成功，绝对路径： " + jsonPath);
    }

    @RequestMapping("/getPicList")
    public Result<?> getPicList(@RequestBody FileManage fileManage){
        LambdaQueryWrapper<FileManage> wrapper = new LambdaQueryWrapper<>();
        if (fileManage.getFileName() != null){
            wrapper.like(FileManage::getFileName,fileManage.getFileName());
        }
        if (fileManage.getFileType() != null){
            wrapper.like(FileManage::getFileType,fileManage.getFileType());
        }
        if (fileManage.getCreateTime() != null){
            wrapper.like(FileManage::getCreateTime,fileManage.getCreateTime());
        }
        return Result.ok(iFileManageService.list(wrapper));
    }

    @PostMapping("/uploadFile")
    public Result<?> uploadFile(@RequestParam("files") MultipartFile[] files) throws Exception {
        //判断文件是否为空
        if (files == null || files.length == 0) {
            return Result.error("请选择上传文件");
        }
        List<FileManage> fileManageList = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;  // 跳过空文件
            }
            String oldName = file.getOriginalFilename();
            //生成新的文件名
            String newName = System.currentTimeMillis() + "_" + oldName;
            try {
                //检查文件是否已经存在
                FileManage existingFile = iFileManageService.getOne(new LambdaQueryWrapper<FileManage>().eq(FileManage::getFileName,oldName));
                if (existingFile != null) {
                    return Result.error("文件已存在，不能上传相同文件: " + oldName);
                }
                //构建文件保存路径
                File newFile = new File(allFilePath + newName);
                //转存文件到指定路径
                file.transferTo(newFile);
                String filename = file.getOriginalFilename();
                String fileType = filename.substring(filename.lastIndexOf(".") + 1);
                String fileAddress = allFilePath + newName;
                BigDecimal fileSize = BigDecimal.valueOf(file.getSize() / 1024f).setScale(2, BigDecimal.ROUND_HALF_UP);
                //创建文件管理对象
                FileManage fileManage = new FileManage();
                fileManage.setFileName(filename);
                fileManage.setFileType(fileType);
                fileManage.setCreateTime(new Date());
                fileManage.setFileAddress(fileAddress);
                fileManage.setFileSize(fileSize);
                fileManageList.add(fileManage);
            } catch (Exception e) {
                e.printStackTrace();
                return Result.error("部分文件上传失败！");
            }
        }
        //批量保存文件信息
        if (!fileManageList.isEmpty()) {
            iFileManageService.saveBatch(fileManageList);
        }
        return Result.ok("文件上传成功!");
    }


}
