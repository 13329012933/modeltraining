import os
import argparse
import numpy as np
import torch
import torch.nn.functional as F
from networks.vnet import VNet

def predict_from_model(savemodel_path, model_path, savedata_path, npy_path, output_path, num_classes, use_cuda):
    net = VNet(n_channels=1, n_classes=num_classes, normalization='batchnorm', has_dropout=False)
    model_full_path = model_path
    net.load_state_dict(torch.load(model_full_path, map_location='cuda' if use_cuda else 'cpu'), strict=True)

    data_full_path = os.path.join(savedata_path, npy_path)
    data = np.load(data_full_path)
    data = data.reshape((1, 1) + data.shape)
    data_tensor = torch.from_numpy(data).float()

    device = torch.device("cuda" if use_cuda else "cpu")
    data_tensor = data_tensor.to(device)
    net = net.to(device)

    with torch.no_grad():
        prediction = net(data_tensor)
        prediction = F.softmax(prediction, dim=1)
        prediction = torch.argmax(prediction, dim=1).squeeze()

    os.makedirs(output_path, exist_ok=True)
    np.save(os.path.join(output_path, npy_path), prediction.cpu().numpy().astype(np.float32))

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--savemodel_path', required=True)
    parser.add_argument('--model_path', required=True)
    parser.add_argument('--savedata_path', required=True)
    parser.add_argument('--npy_path', required=True)
    parser.add_argument('--output_path', required=True)
    parser.add_argument('--num_classes', type=int, default=2)
    parser.add_argument('--use_cuda', action='store_true')  # 传入 --use_cuda 启用GPU

    args = parser.parse_args()

    predict_from_model(
        args.savemodel_path,
        args.model_path,
        args.savedata_path,
        args.npy_path,
        args.output_path,
        args.num_classes,
        args.use_cuda
    )
