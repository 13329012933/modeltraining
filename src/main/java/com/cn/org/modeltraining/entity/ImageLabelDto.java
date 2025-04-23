package com.cn.org.modeltraining.entity;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ImageLabelDto {
    private Long id;
    private String npyName;
    private String dimension;
    private String fileAddress;
    private String labelName;
    private List<List<Map<String, Double>>> points; //二维列表
}
