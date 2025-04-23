package com.cn.org.modeltraining.utils;

import org.apache.commons.compress.utils.IOUtils;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FileToMultipartFileUtil {
    public static MultipartFile convert(File file) throws IOException {
        FileInputStream input = new FileInputStream(file);
        MultipartFile multipartFile = new MockMultipartFile(
                file.getName(),
                file.getName(),
                "image/png",
                IOUtils.toByteArray(input));
        return multipartFile;
    }
}
