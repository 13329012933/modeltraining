import sys
import numpy as np
import json
import os

def save_npy(json_file_path, output_dir, file_name):
    try:
        # 读取 JSON 数据
        with open(json_file_path, "r", encoding="utf-8") as f:
            array = json.load(f)

        # 转换为 NumPy 数组
        np_array = np.array(array, dtype=np.int32)

        # 确保输出目录存在
        os.makedirs(output_dir, exist_ok=True)

        # 拼接完整的 .npy 文件路径
        output_path = os.path.join(output_dir, file_name if file_name.endswith(".npy") else file_name + ".npy")

        # 保存 .npy 文件
        np.save(output_path, np_array)
        print(f"成功保存 .npy 文件: {output_path}")

    except Exception as e:
        print(f"保存 .npy 文件失败: {e}")
        sys.exit(1)

if __name__ == "__main__":
    if len(sys.argv) != 4:
        print("用法：python save_npy.py <json路径> <输出目录> <保存文件名>")
        sys.exit(1)

    json_file_path = sys.argv[1]
    output_dir = sys.argv[2]
    file_name = sys.argv[3]

    save_npy(json_file_path, output_dir, file_name)
