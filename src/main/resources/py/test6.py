import sys
import json
import numpy as np
import PIL.ExifTags
import PIL.Image
import PIL.ImageOps
import base64
import io
import os
import cv2


# 读取JSON模板文件
def read_json_template(file_path):
    with open(file_path, 'r', encoding='utf-8') as file:
        data = json.load(file)
    return data


# 在JSON数据中添加内容
def add_content_to_json(data, keys, new_contents):
    if len(keys) != len(new_contents):
        raise ValueError("Keys and new contents must have the same length.")
    for key, new_content in zip(keys, new_contents):
        data[key] = new_content
    return data


def save_json_data(data, file_path):
    directory = os.path.dirname(file_path)
    if directory and not os.path.exists(directory):
        os.makedirs(directory)
    if not file_path.endswith('.json'):
        file_path += '.json'
    with open(file_path, 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=4, ensure_ascii=False)


def gen_dict(origin_list):
    new_list = [[[d['x'], d['y']] for d in sublist] for sublist in origin_list]
    dict_list = []
    for idx, one_sam in enumerate(new_list):
        new_dict = {"label": '0', "points": one_sam, "group_id": "", "description": "",
                    "shape_type": "polygon", "flags": {}, "mask": ""}
        dict_list.append(new_dict)
    return dict_list


def img_arr_to_b64(img_path):
    img_pil = PIL.Image.open(img_path).convert("RGB")  # 转换为 RGB 避免 RGBA 错误
    img_arr = np.array(img_pil)
    img_pil = PIL.Image.fromarray(img_arr)

    f = io.BytesIO()
    img_pil.save(f, format="JPEG")  # 现在 `JPEG` 只会接收 `RGB` 格式
    img_bin = f.getvalue()

    if hasattr(base64, "encodebytes"):
        img_b64 = base64.encodebytes(img_bin)
    else:
        img_b64 = base64.encodestring(img_bin)

    imagedata = str(base64.b64encode(img_bin).decode("utf-8"))
    image_width, image_height = img_pil.size

    return imagedata, image_width, image_height


# 主函数：生成 JSON 文件
def write_2_json(template_file_path, image_path, polygons):

    # 遍历图片文件夹
    assert image_path.lower().endswith(('.png', '.jpg', '.jpeg', '.bmp', '.tiff'))
    img_name = image_path.split('/')[-1]
    # 生成 JSON 文件名
    os.makedirs('json_folder', exist_ok=True)
    json_file_name = os.path.splitext(img_name)[0] + '.json'
    json_file_full_path = os.path.join('json_folder', json_file_name)
    # 读取模板文件
    data0 = read_json_template(template_file_path)
    dict_list = gen_dict(polygons)
    # 获取图片的 Base64 编码
    imagedata, image_width, image_height = img_arr_to_b64(image_path)

    # 更新 JSON 数据
    key_list = ["shapes", "imagePath", "imageData", "imageHeight", "imageWidth"]
    value_list = [dict_list, img_name, imagedata, image_height, image_width]
    data = add_content_to_json(data0, key_list, value_list)

    # 保存 JSON 文件
    save_json_data(data, json_file_full_path)
    return json_file_full_path


# 处理单个 JSON 文件并生成对应的 mask 图像
def func(json_file):
    with open(json_file, mode='r', encoding="utf-8") as f:
        configs = json.load(f)
    shapes = configs["shapes"]
    png = np.zeros((configs["imageHeight"], configs["imageWidth"], 3), np.uint8)
    for shape in shapes:
        points = [point for point in shape["points"]]  # 直接获取点列表
        contour = [np.array(points, dtype=np.int32)]  # 将点列表转换为 np.array，并放在一个列表中
        if contour is not None and len(contour) > 0:
            cv2.drawContours(png, contour, -1, (255, 255, 255), -1)
    hsv = cv2.cvtColor(png, cv2.COLOR_BGR2HSV)
    # 应用颜色阈值
    lower_light_pink = np.array([0, 0, 0])  # 浅粉色的最低色调，最低饱和度和最低亮度
    upper_light_pink = np.array([255,255,255])  # 浅粉色的最高色调，最高饱和度和最高亮度
    mask = cv2.inRange(hsv, lower_light_pink, upper_light_pink)
    # 使用Canny算法找到边缘
    edges = cv2.Canny(mask, 50, 150)
    # 创建白色图片
    white_image = np.zeros_like(png)
    white_image[:] = (255, 255, 255)
    # 将边缘填充到白色图片中
    white_edges = cv2.bitwise_and(white_image, white_image, mask=edges)
    # 叠加显示原图和处理后的边缘
    combined = cv2.addWeighted(png, 1.0, white_edges, 1.0, 0.0)
    return combined



# 遍历指定文件夹中的所有 JSON 文件并处理
def dir_func(json_file):
    if json_file.endswith('.json'):  # 确保处理的是JSON文件
        mask_image = func(json_file)
    else:
        print("The specified directory does not exist.")
        mask_image = []
    return mask_image


if __name__ == '__main__':
    if len(sys.argv) != 4:
        sys.exit(1)
    template_path = sys.argv[1]  # JSON 模板文件路径
    image_path = sys.argv[2]  # 图片路径
    polygons = sys.argv[3]  # 标点的数据

    if isinstance(polygons, str):  # 解决 JSON 字符串问题
        polygons = json.loads(polygons)
    # 调用主函数生成 JSON 文件
    json_path = write_2_json(template_path, image_path, polygons)
    # 处理生成的 JSON 文件并生成判别结果
    mask_image_ = dir_func(json_path)
    mask_image_=(mask_image_ > 0).astype(np.uint8)
    mask_image = mask_image_[:, :, 0]
    # mask_image是NumPy数组
    mask_list = mask_image.tolist()  # 转换为 list
    output_json = json.dumps({"json_path": json_path, "mask_image": mask_list})
    print(output_json)  # 直接输出 JSON
