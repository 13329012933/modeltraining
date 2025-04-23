package com.cn.org.modeltraining.utils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * @Description: 通用工具
 */
@Slf4j
public class CommonUtils {

    /**
     * 文件名 正则字符串
     * 文件名支持的字符串：字母数字中文.-_()（） 除此之外的字符将被删除
     */
    private static String FILE_NAME_REGEX = "[^A-Za-z\\.\\(\\)\\-（）\\_0-9\\u4e00-\\u9fa5]";



    /**
     * 判断文件名是否带盘符，重新处理
     * @param fileName
     * @return
     */
    public static String getFileName(String fileName){
        //判断是否带有盘符信息
        // Check for Unix-style path
        int unixSep = fileName.lastIndexOf('/');
        // Check for Windows-style path
        int winSep = fileName.lastIndexOf('\\');
        // Cut off at latest possible point
        int pos = (winSep > unixSep ? winSep : unixSep);
        if (pos != -1)  {
            // Any sort of path separator found...
            fileName = fileName.substring(pos + 1);
        }
        //替换上传文件名字的特殊字符
        fileName = fileName.replace("=","").replace(",","").replace("&","")
                .replace("#", "").replace("“", "").replace("”", "");
        //替换上传文件名字中的空格
        fileName=fileName.replaceAll("\\s","");
        //在线表单 使用文件组件时，上传文件名中含%，下载异常
        fileName = fileName.replaceAll(FILE_NAME_REGEX, "");
        //在线表单 使用文件组件时，上传文件名中含%，下载异常
        return fileName;
    }

    public static void toCopyFile(String filePath,String targetPath){
        // 源文件路径
        Path sourceFile = Paths.get(filePath);
        // 目标文件夹路径（文件会被复制到这个文件夹下）
        Path targetDirectory = Paths.get(targetPath);
        // 确保目标文件夹存在，不存在就创建
        try {
            if (!Files.exists(targetDirectory)) {
                Files.createDirectories(targetDirectory);
            }
            // 构造目标文件路径（即目标文件夹 + 源文件名）
            Path targetFile = targetDirectory.resolve(sourceFile.getFileName());
            // 复制文件，若已存在则覆盖
            Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
