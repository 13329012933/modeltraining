package com.cn.org.modeltraining.entity;

import lombok.Data;

@Data
public class ParamDTO {
    private String exp;
    private Integer max_iterations;
    private Integer batch_size;
    private Integer labeled_bs;
    private Double base_lr;
    private Integer deterministic;
    private Integer seed;
    private Integer sliceseed;
    private String gpu;
    private String split;
    private Integer num;
    private Double quality_bar;
    private Double ht;
    private Double st;
    private Double ema_decay;
    private Integer slice_strategy;
    private String consistency_type;
    private Double consistency;
    private Double consistency_rampup;
    private Integer init;
}
