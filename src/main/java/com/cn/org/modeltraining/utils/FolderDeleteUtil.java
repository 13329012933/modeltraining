package com.cn.org.modeltraining.utils;

import java.io.File;

public class FolderDeleteUtil {

    /**
     * 删除指定父目录下名为 targetName 的文件夹（及其内容），保留其他文件/目录。
     *
     * @param parentDirPath 父目录路径，例如 "D:/data/train/x"
     * @param targetFolderName 要删除的子文件夹名，例如 "data"
     */
    public static void deleteSpecificSubfolder(String parentDirPath, String targetFolderName) {
        File parentDir = new File(parentDirPath);
        if (!parentDir.exists() || !parentDir.isDirectory()) {
            System.out.println("父目录无效: " + parentDirPath);
            return;
        }

        File targetFolder = new File(parentDir, targetFolderName);
        if (targetFolder.exists() && targetFolder.isDirectory()) {
            deleteRecursively(targetFolder);
            System.out.println("已删除文件夹: " + targetFolder.getAbsolutePath());
        } else {
            System.out.println("未找到目标文件夹: " + targetFolder.getAbsolutePath());
        }
    }

    //递归删除文件或文件夹
    private static void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] contents = file.listFiles();
            if (contents != null) {
                for (File f : contents) {
                    deleteRecursively(f);
                }
            }
        }
        if (!file.canWrite()) {
            file.setWritable(true);
        }

        if (!file.delete()) {
            System.out.println("删除失败: " + file.getAbsolutePath());
        } else {
            System.out.println("已删除: " + file.getAbsolutePath());
        }
    }

}
