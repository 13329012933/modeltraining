package com.cn.org.modeltraining.utils;

import java.io.*;

public class PythonExecutor {
    public static void main(String[] args) {
        try {
            // Python 脚本路径（可以修改为你的 Python 脚本文件）
            String pythonScript = "D:\\GiteeDemo\\modeltraining\\src\\main\\resources\\py\\test01.py";
            // 创建 ProcessBuilder 运行 Python
            ProcessBuilder pb = new ProcessBuilder("python", pythonScript);
            pb.redirectErrorStream(true); //合并标准错误输出
            Process process = pb.start(); //启动进程
            // 读取 Python 输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("Python Output: " + line);
            }
            // 等待进程结束
            int exitCode = process.waitFor();
            System.out.println("Python 进程退出码: " + exitCode);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
