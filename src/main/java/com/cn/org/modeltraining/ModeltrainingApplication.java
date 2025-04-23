package com.cn.org.modeltraining;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.bind.annotation.CrossOrigin;

@SpringBootApplication
@ServletComponentScan
@EnableTransactionManagement
@EnableCaching
@EnableScheduling
@CrossOrigin(allowCredentials = "true", allowedHeaders = "*")
public class ModeltrainingApplication {

    public static void main(String[] args) {
        SpringApplication.run(ModeltrainingApplication.class, args);
    }

}
