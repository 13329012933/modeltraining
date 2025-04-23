# -*- coding: utf-8 -*-
import os
import sys
import numpy as np
import matplotlib.pyplot as plt
from PIL import Image

def save_slices(npy_file, step=10, target_size=(128, 128)):
    # 读取.npy文件
    data = np.load(npy_file)
    if data.ndim != 3:
        raise ValueError("数据维度必须为3维")

    base_name = os.path.splitext(os.path.basename(npy_file))[0]
    output_dir = os.path.join(os.path.dirname(npy_file), "data", base_name)

    # 保存切片函数
    def save_slice(slice_data, path, slice_index):
        os.makedirs(path, exist_ok=True)
        plt.imshow(slice_data, cmap='gray')
        plt.axis('off')

        temp_path = os.path.join(path, f"{slice_index}_temp.png")
        final_path = os.path.join(path, f"{slice_index}.png")

        plt.savefig(temp_path, bbox_inches='tight', pad_inches=0)
        plt.close()

        # 使用新方式缩放图像
        img = Image.open(temp_path).convert('L')
        img = img.resize(target_size, Image.Resampling.BILINEAR)
        img.save(final_path)
        os.remove(temp_path)

    # 垂直切片
    for i in range(0, data.shape[0], step):
        save_slice(data[i, :, :], os.path.join(output_dir, "Vertical"), i)

    # Inline 切片
    for i in range(0, data.shape[1], step):
        save_slice(data[:, i, :], os.path.join(output_dir, "Inline"), i)

    # Crossline 切片
    for i in range(0, data.shape[2], step):
        save_slice(data[:, :, i], os.path.join(output_dir, "Crossline"), i)

    print(f"切片保存完成，大小：{target_size}，位置：{output_dir}")

if __name__ == "__main__":
    # 检查参数是否正确
    if len(sys.argv) < 3:
        print("用法: python npy2img.py <npy文件路径> <步长>")
        sys.exit(1)

    npy_file_path = sys.argv[1]  # 获取第一个参数（npy文件路径）
    step = int(sys.argv[2])  # 获取第二个参数（步长）

    if not os.path.exists(npy_file_path):
        print(f"错误: 文件 {npy_file_path} 不存在")
        sys.exit(1)

    save_slices(npy_file_path, step, target_size=(128, 128))
