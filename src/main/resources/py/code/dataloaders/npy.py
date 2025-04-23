import os
import torch
import numpy as np
from glob import glob
from torch.utils.data import Dataset
import itertools
from torch.utils.data.sampler import Sampler
from torchvision.transforms import Compose

import pdb


class NpyDataset_Sparse(Dataset):
    """
    Load_Dataset
    """

    def __init__(self, path, mode='train', num=None, slice_strategy=1, init=0, transform=None):
        self.path = path
        self.transform = transform
        self.mode = mode
        self.image_list, self.label_list = self.load_data()
        #self.slice_random=np.random.randint(0,slice_strategy,num)#生成一个取值范围是 [0, slice_strategy)长度为num的随机数数组，获取每个训练样本的稀疏切片标签时加一个随机偏移量
        self.slice_random= np.full(num, init)
        print(self.slice_random)
        self.slice1=[i for i in list(range(0,128,slice_strategy))]
        self.slice2=[i for i in list(range(0,128,slice_strategy))]
        if num is not None:
            self.image_list = self.image_list[:num]########################选多少样本
            print(self.image_list)
        print("total {} samples".format(len(self.image_list)))

    def __getitem__(self, index):
        image = np.load(self.image_list[index])
        if len(self.label_list) == 0:
            label = np.zeros(image.shape)
        else:
            labeltmp = np.load(self.label_list[index])
        label = np.zeros_like(labeltmp) #初始化稀疏标签
        slice1 = [i + self.slice_random[index] for i in self.slice1]
        slice2 = [i + self.slice_random[index] for i in self.slice2]
        for i in slice1:
            label[:,:,i]=labeltmp[:,:,i]
        for i in slice2:
            label[:,i,:]=labeltmp[:,i,:]
        #数据标准化过了，就不再标准化了
        img = image
        if len(img.shape) == 3:
            img = img.reshape((1, img.shape[0], img.shape[1], img.shape[2]))
        x = (torch.from_numpy(img)).reshape(128,128,128)
        y = (torch.from_numpy(label)).reshape(128, 128, 128)
        data = {'x': x.float(), 'y': y.float(), 'slice1': slice1, 'slice2':slice2}
        if self.transform:
            data = self.transform(data)
        return data

    def __len__(self):
        return len(self.image_list)

    def load_data(self):
        """

        :return:
        """
        img_list = []
        label_list = []
        label_pred_list = []
        img_path = os.path.join(self.path, 'x/')
        label_path = os.path.join(self.path, 'y/')
        for item in os.listdir(img_path):
            img_list.append(os.path.join(img_path, item))
            # 由于x和y的文件名一样，所以用一步加载进来
            label_list.append(os.path.join(label_path, item))
        if self.mode != 'pred':
            return img_list, label_list
        else:
            return img_list, label_pred_list


class NpyRandomCrop_Sparse(object):
    """
    Crop randomly the image in a sample
    Args:
    output_size (int): Desired output size
    """

    def __init__(self, output_size):
        self.output_size = output_size

    def __call__(self, sample):
        image, label,slice1,slice2 = sample['x'], sample['y'],sample['slice1'],sample['slice2']
        #image, label, slice1 = sample['image'], sample['label'], sample['slice1']
        # pad the sample if necessary
        if label.shape[0] <= self.output_size[0] or label.shape[1] <= self.output_size[1] or label.shape[2] <= \
                self.output_size[2]:
            pw = max((self.output_size[0] - label.shape[0]) // 2 + 3, 0)
            ph = max((self.output_size[1] - label.shape[1]) // 2 + 3, 0)
            pd = max((self.output_size[2] - label.shape[2]) // 2 + 3, 0)
            image = np.pad(image, [(pw, pw), (ph, ph), (pd, pd)], mode='constant', constant_values=0)
            label = np.pad(label, [(pw, pw), (ph, ph), (pd, pd)], mode='constant', constant_values=0)

        (w, h, d) = image.shape
        # if np.random.uniform() > 0.33:
        #     w1 = np.random.randint((w - self.output_size[0])//4, 3*(w - self.output_size[0])//4)
        #     h1 = np.random.randint((h - self.output_size[1])//4, 3*(h - self.output_size[1])//4)
        # else:
        w1 = np.random.randint(0, w - self.output_size[0])
        h1 = np.random.randint(0, h - self.output_size[1])
        d1 = np.random.randint(0, d - self.output_size[2])
        weight = np.zeros_like(image)
        label = label[w1:w1 + self.output_size[0], h1:h1 + self.output_size[1], d1:d1 + self.output_size[2]]
        image = image[w1:w1 + self.output_size[0], h1:h1 + self.output_size[1], d1:d1 + self.output_size[2]]
        #weight[:]=1

        for i in slice1:
            weight[:, :, i+3] = 1
        for i in slice2:
            weight[:,i,:]=1

        weight = weight[w1:w1 + self.output_size[0], h1:h1 + self.output_size[1], d1:d1 + self.output_size[2]]
        return {'x': image, 'y': label, 'weight': weight}


class ToTensor(object):
    """Convert ndarrays in sample to Tensors."""

    def __call__(self, sample):
        image = sample['x']
        image = image.reshape(1, image.shape[0], image.shape[1], image.shape[2]).astype(np.float32)
        if 'onehot_label' in sample:
            return {'x': torch.from_numpy(image), 'y': torch.from_numpy(sample['y']).long(),
                    'onehot_y': torch.from_numpy(sample['onehot_y']).long()}
        elif 'weight' in sample:
            return {'x': torch.from_numpy(image), 'y': torch.from_numpy(sample['y']).long(),
                    'weight': torch.from_numpy(sample['weight'])}
        else:
            return {'x': torch.from_numpy(image), 'y': torch.from_numpy(sample['y']).long()}
