package com.cn.org.modeltraining.configuration;

import com.cn.org.modeltraining.commom.CommonConstant;
import com.cn.org.modeltraining.commom.SymbolConstant;
import com.cn.org.modeltraining.utils.MinioUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MinioConfig {
    @Value(value = "${minio.minio_url}")
    private String minioUrl;
    @Value(value = "${minio.minio_name}")
    private String minioName;
    @Value(value = "${minio.minio_pass}")
    private String minioPass;
    @Value(value = "${minio.bucketName}")
    private String bucketName;

    @Bean
    public void initMinio(){
        if(!minioUrl.startsWith(CommonConstant.STR_HTTP)){
            minioUrl = "http://" + minioUrl;
        }
        if(!minioUrl.endsWith(SymbolConstant.SINGLE_SLASH)){
            minioUrl = minioUrl.concat(SymbolConstant.SINGLE_SLASH);
        }
        MinioUtil.setMinioUrl(minioUrl);
        MinioUtil.setMinioName(minioName);
        MinioUtil.setMinioPass(minioPass);
        MinioUtil.setBucketName(bucketName);
    }

}
