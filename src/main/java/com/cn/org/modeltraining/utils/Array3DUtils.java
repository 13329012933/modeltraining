package com.cn.org.modeltraining.utils;

public class Array3DUtils {

    // 主方法：将输入数组从给定方向转换为 [vertical][inline][crossline] 方向
    public static int[][][] transposeToVerticalInlineCrossline(int[][][] input, String fromDirection) {
        switch (fromDirection.toLowerCase()) {
            case "vertical":
                // 已经是 [vertical][inline][crossline]，不需要转换
                return input;
            case "inline":
                // 从 [inline][vertical][crossline] -> [vertical][inline][crossline]
                return reorderAxes(input, 1, 0, 2); // i,j,k -> j,i,k
            case "crossline":
                // 从 [crossline][inline][vertical] -> [vertical][inline][crossline]
                return reorderAxes(input, 1, 2, 0); // i,j,k -> k,j,i
            default:
                throw new IllegalArgumentException("未知方向: " + fromDirection);
        }
    }

    // 通用维度重排方法：输入轴顺序 mapping[0]=i, mapping[1]=j, mapping[2]=k 对应原始坐标轴
    public static int[][][] reorderAxes(int[][][] input, int axisI, int axisJ, int axisK) {
        int[] shape = new int[]{input.length, input[0].length, input[0][0].length};
        int[][][] output = new int[shape[axisI]][shape[axisJ]][shape[axisK]];

        for (int i = 0; i < shape[0]; i++) {
            for (int j = 0; j < shape[1]; j++) {
                for (int k = 0; k < shape[2]; k++) {
                    int[] indices = new int[]{i, j, k};
                    int i2 = indices[axisI];
                    int j2 = indices[axisJ];
                    int k2 = indices[axisK];
                    output[i2][j2][k2] = input[i][j][k];
                }
            }
        }
        return output;
    }
}
