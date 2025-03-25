package com.cn.org.modeltraining.controller;


import com.alibaba.fastjson.JSONObject;
import com.cn.org.modeltraining.commom.Result;
import com.cn.org.modeltraining.utils.JsonToFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @RequestMapping("/test01")
    public Result<?> getPicCoordinate(@RequestBody JSONObject jsonObject){
        String fileName = jsonObject.getString("fileName");
        jsonObject.remove("fileName");
        String jsonString = jsonObject.toJSONString();
        String jsonPath = JsonToFile.toJsonFile(jsonString, fileDir, fileName);
        return Result.ok("JSON文件生成成功，绝对路径： " + jsonPath);
    }

    @RequestMapping("/test02")
    public Result<?> getPicList(@RequestBody JSONObject jsonObject){
        return Result.ok();
    }
}
