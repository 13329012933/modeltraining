package com.cn.org.modeltraining.utils;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.IOException;

public class JsonToFile {

    public static String toJsonFile(String jsonString, String path, String fileName){
        StringBuffer heade = new StringBuffer(path);
        heade.append("/" + fileName +".json");
        String resPath = heade.toString();
        try {
            //创建ObjectMapper对象
            ObjectMapper objectMapper = new ObjectMapper();
            //将JSON字符串转换为Map（或对象）
            Object obj = objectMapper.readValue(jsonString, Object.class);
            //将对象写入JSON文件
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(resPath), obj);
            System.out.println("JSON文件已保存：" + resPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resPath;
    }
}
