"""
需要上传的参数 num, slice_strategy, init, project_name
"""
"""
需要上传的参数 num, slice_strategy, init, project_name
"""
num=200 #数据数量
slice_strategy=8 #标注的切片步长
init=0 #标注时的起始切片
project_name="projectA"

import os
import json
import argparse
import sys
from tqdm import tqdm
import logging
import time
import random
import numpy as np
import torch
import torch.optim as optim
from torchvision import transforms
from torch.nn import DataParallel
import torch.nn.functional as F
import torch.backends.cudnn as cudnn
from torch.utils.data import DataLoader
from networks.vnet import VNet
from networks.unet import UNet
from utils import ramps, losses
from dataloaders.npy import NpyDataset_Sparse, NpyRandomCrop_Sparse,ToTensor

parser = argparse.ArgumentParser()
# 所有参数统一在一起
parser.add_argument('--snapshot_path', type=str, help='路径参数（可选）')
parser.add_argument('--dataset', type=str, default='合成数据', help='dataset to use')
parser.add_argument('--max_iterations', type=int, default=6000, help='maximum epoch number to train')
parser.add_argument('--batch_size', type=int, default=1, help='batch_size per gpu')
parser.add_argument('--labeled_bs', type=int, default=1, help='labeled_batch_size per gpu')
parser.add_argument('--base_lr', type=float, default=0.001, help='maximum epoch number to train')
parser.add_argument('--deterministic', type=int, default=1, help='whether use deterministic training')
parser.add_argument('--seed', type=int, default=1337, help='random seed')
parser.add_argument('--sliceseed', type=int, default=0, help='random seed')
parser.add_argument('--gpu', type=str, default='0, 1', help='GPU to use')
parser.add_argument('--split', type=str,  default='train', help='datalist to use')
parser.add_argument('--quality_bar', type=float,  default=0.98, help='quality bar')
parser.add_argument('--ht', type=float,  default=0.9, help='hard threshold')
parser.add_argument('--st', type=float,  default=0.7, help='soft threshold')
parser.add_argument('--ema_decay', type=float, default=0.99, help='ema_decay')
parser.add_argument('--consistency_type', type=str, default="mse", help='consistency_type')
parser.add_argument('--consistency', type=float, default=0.1, help='consistency')
parser.add_argument('--consistency_rampup', type=float, default=40.0, help='consistency_rampup')

# 只调用一次解析
args, unknown = parser.parse_known_args()

# Java 可能直接传路径，不带参数名
snapshot_path = args.snapshot_path or (sys.argv[1] if len(sys.argv) > 1 else None)

if snapshot_path is None or not os.path.exists(snapshot_path):
    sys.exit(1)

# os.environ['CUDA_VISIBLE_DEVICES'] = args.gpu
# gpus=[0, 1]
snapshot_path = os.path.join(snapshot_path)
train_data_path = os.path.join(snapshot_path,"data/train") #存放训练数据位置
batch_size = args.batch_size
max_iterations = args.max_iterations
base_lr = args.base_lr
labeled_bs = args.labeled_bs

json_path = os.path.join(snapshot_path, 'params.json')  # 拼成完整路径
with open(json_path,'r',encoding='utf-8') as f:
    arg = json.load(f)
num=arg["num"] #数据数量
slice_strategy=arg["slice_strategy"] #标注的切片步长
init=arg["init"] #标注时的起始切片
project_name=arg["exp"]

def get_current_consistency_weight(epoch):
    # Consistency ramp-up from https://arxiv.org/abs/1610.02242
    return args.consistency * ramps.sigmoid_rampup(epoch, args.consistency_rampup)

def update_ema_variables(model, ema_model, alpha, global_step):
    # Use the true average until the exponential average is more correct
    alpha = min(1 - 1 / (global_step + 1), alpha)
    for ema_param, param in zip(ema_model.parameters(), model.parameters()):
        ema_param.data.mul_(alpha).add_(1 - alpha, param.data)

if __name__ == "__main__":
    ## make logger file
    if not os.path.exists(snapshot_path):
        os.makedirs(snapshot_path)
    logging.basicConfig(filename=snapshot_path + "/log.txt", level=logging.INFO,
                        format='[%(asctime)s.%(msecs)03d] %(message)s', datefmt='%H:%M:%S')
    logging.getLogger().addHandler(logging.StreamHandler(sys.stdout))
    logging.info(str(args))

    if args.deterministic:
        cudnn.benchmark = False
        cudnn.deterministic = True
        random.seed(args.seed)
        np.random.seed(args.seed)
        torch.manual_seed(args.seed)
        torch.cuda.manual_seed(args.seed)
    np.random.seed(args.sliceseed)

    if args.dataset == '合成数据':
        num_classes = 2
        patch_size = (128, 128, 128)
        #稀疏数据集
        db_train1 = NpyDataset_Sparse(path=train_data_path,
                                    mode=args.split,
                                    num=num,
                                    slice_strategy=slice_strategy,
                                    init=init,
                                    transform=transforms.Compose([
                                        NpyRandomCrop_Sparse(patch_size), #计算权重矩阵，进行随机裁剪
                                        #MMRandomRotFlip(),
                                        ToTensor(),
                                    ]))
    np.random.seed(args.seed)

    def create_3dmodel(ema=False):
        # Network definition
        net = VNet(n_channels=1, n_classes=num_classes, normalization='batchnorm', has_dropout=True)
        model = net.cpu()
        return model

    def create_2dmodel(ema=False):
        # Network definition
        net = UNet(n_channels=1, n_classes=num_classes)
        model = net.cpu()
        return model

    model1 = create_3dmodel().cpu()
    #model1 = DataParallel(model1, device_ids=gpus, output_device=gpus[0])
    model2 = create_2dmodel().cpu()
    #model2 = create_2dmodel()
    #model2 = DataParallel(model2, device_ids=gpus, output_device=gpus[0])
    model3 = create_2dmodel().cpu()
    #model3 = DataParallel(model3, device_ids=gpus, output_device=gpus[0])

    def worker_init_fn(worker_id):
        random.seed(args.seed + worker_id)
                                                                               #服务器上用8
    trainloader1 = DataLoader(db_train1, batch_size=batch_size, shuffle=True,  num_workers=0, pin_memory=False,  worker_init_fn=worker_init_fn)
    model1.train()
    model2.train()
    model3.train()
    optimizer1 = optim.Adam(model1.parameters(), lr=base_lr, betas=(0.9, 0.999), eps=1e-8, weight_decay=0.0001) #全精度时eps设定为1e-8， 半精度时eps设定为1e-5
    optimizer2 = optim.Adam(model2.parameters(), lr=base_lr, betas=(0.9, 0.999), eps=1e-8, weight_decay=0.0001)
    optimizer3 = optim.Adam(model3.parameters(), lr=base_lr, betas=(0.9, 0.999), eps=1e-8, weight_decay=0.0001)
    logging.info("{} itertations per epoch".format(len(trainloader1)))

    iter_num = 0
    max_epoch = max_iterations // len(trainloader1) + 1
    lr_ = base_lr
    for epoch_num in tqdm(range(max_epoch), ncols=70):
        time1 = time.time()
        for i_batch, sampled_batch1 in enumerate(trainloader1):
            time2 = time.time()
           # print('fetch data cost {}'.format(time2-time1))
           #  volume_batch1, label_batch1, maskz1 = sampled_batch1['x'], sampled_batch1['y'], sampled_batch1['weight']
           #  maskz1 = maskz1.cuda()
           #  maskzz1 = torch.unsqueeze(maskz1, 1).cuda()
           #  maskzz1 = maskzz1.repeat(1, 1, 1, 1, 1).cuda()
           #  volume_batch1, label_batch1 = volume_batch1.cuda(), label_batch1.cuda()
           #  outputs1 = model1(volume_batch1) #这里只占用cuda0，因为batchsize是1,  torch.Size([1, 2, 128, 128, 128])
           #  outputs_soft1 = F.softmax(outputs1, dim=1)   #torch.Size([1, 2, 128, 128, 128])

            volume_batch1, label_batch1, maskz1 = sampled_batch1['x'], sampled_batch1['y'], sampled_batch1['weight']
            # 将数据送入CUDA
            maskz1 = maskz1.cpu()
            maskzz1 = torch.unsqueeze(maskz1, 1).cpu()
            maskzz1 = maskzz1.repeat(1, 1, 1, 1, 1).cpu()
            volume_batch1, label_batch1 = volume_batch1.cpu(), label_batch1.cpu()
            # 将输入数据切割成4个块 (沿第0维进行分割，batch_size=4)
            volume_batch_split = torch.chunk(volume_batch1, 4, dim=2)
            # 直接使用batch_size=4一起输入
            # 将这些块合并为一个batch_size=4的输入
            volume_batch4 = torch.cat(volume_batch_split, dim=0)  # 合并成一个batch to+rch.Size([4, 1, 32, 128, 128])
            # 输入模型进行推理
            outputs1 = model1(volume_batch4)  # 使用batch_size=4进行推理
            # 保持和原来代码一致的输出处理
            outputs1 = torch.cat([outputs1[0,:,:,:,:],outputs1[1,:,:,:,:],outputs1[2,:,:,:,:],outputs1[3,:,:,:,:]], dim=1).unsqueeze(0) #torch.Size([1, 2, 128, 128, 128])
            outputs_soft1 = F.softmax(outputs1, dim=1)  # 对输出进行softmax

            # gc.collect()
            # torch.cuda.empty_cache()
            # # 查看当前系统可用的 GPU 数量
            # num_gpus = torch.cuda.device_count()
            # print(f"Number of available GPUs: {num_gpus}")
            # # 打印每个 GPU 的显存使用情况
            # for i in range(num_gpus):
            #     total_memory = torch.cuda.get_device_properties(i).total_memory / 1024 ** 3  # 转换为 GB
            #     allocated_memory = torch.cuda.memory_allocated(i) / 1024 ** 3
            #     cached_memory = torch.cuda.memory_reserved(i) / 1024 ** 3
            #     print(f"GPU {i}: {torch.cuda.get_device_name(i)}")
            #     print(f"  Total Memory: {total_memory:.2f} GB")
            #     print(f"  Memory Allocated: {allocated_memory:.2f} GB")
            #     print(f"  Memory Cached: {cached_memory:.2f} GB")

            volume_batch2 = volume_batch1[0].transpose(0, 3).squeeze().unsqueeze(1) #[1,1,192,192,96]->[96,1,192,192]
            #print(torch.sum(outputs_soft1[:,1]>0.5))
            outputs2 = model2(volume_batch2)  # [96,8,192,192]
            outputs_soft2 = F.softmax(outputs2, dim=1)
            outputs2 = outputs2.unsqueeze(4).transpose(0, 4) #[1,8,192,192,96]
            outputs_soft2 = outputs_soft2.unsqueeze(4).transpose(0, 4)

            volume_batch3=volume_batch1[0].transpose(0, 2).squeeze().unsqueeze(1)#[128,1,128,128]
            outputs3=model3(volume_batch3.cpu()).cpu() #[128,8,128,128]
            outputs_soft3=F.softmax(outputs3,dim=1)
            outputs3=outputs3.unsqueeze(3).transpose(0, 3)
            outputs_soft3=outputs_soft3.unsqueeze(3).transpose(0, 3)

            twodthreshold=0.0
            twod1=torch.argmax(outputs_soft2.detach(), dim=1, keepdim=False)
            confidence2d1,_=torch.max(outputs_soft2.detach(), dim=1, keepdim=False)
            twod2=torch.argmax(outputs_soft3.detach(),dim=1,keepdim=False)
            confidence2d2,_=torch.max(outputs_soft3.detach(), dim=1, keepdim=False)
            threed = torch.argmax(outputs_soft1.detach(), dim=1, keepdim=False)
            confidence3d,_ = torch.max(outputs_soft1.detach(), dim=1, keepdim=False) #每个像素点的置信度，即预测结果的概率
            threedcorrection=(twod1!=threed)*(confidence3d>confidence2d1)*(confidence3d>confidence2d2)#2D和3D预测结果不同，且3D置信更高的位置
            threedcorrection=~threedcorrection #2D和3D预测结果相同，或2D置信更高的位置
            twodmask=(twod1==twod2)*threedcorrection*(confidence2d1>twodthreshold)*(confidence2d2>twodthreshold)
                    #两个2D预测结果一致&2D比3D更好的位置&后面两个条件没用到
            hardthreedthreshold = args.ht
            softthreedthreshold = args.st
            threedmask = confidence3d > hardthreedthreshold
            twodcorrection1 = (confidence2d1 > confidence3d) * (twod1 != threed) #2D和3D预测结果不同，且2D1置信更高的位置
            twodcorrection1 = ~twodcorrection1 #2D和3D预测结果相同，或3D置信更高的位置
            threedmask1 = threedmask# 置信大于硬阈值的位置
            twodcorrection2 = (confidence2d2 > confidence3d) * (twod2 != threed) #2D和3D预测结果不同，且2D2置信更高的位置
            twodcorrection2 = ~twodcorrection2 #2D和3D预测结果相同，或3D置信更高的位置
            threedmask2 = threedmask
            consistency_weight = get_current_consistency_weight(iter_num // 150) #权重从0逐步增大到0.1
            # 3d quality verification and good sample selection
            print(threed.shape)
            print(label_batch1.shape)
            print(
                np.count_nonzero(threed[maskz1 == 1].cpu().numpy() == label_batch1[maskz1 == 1].cpu().numpy()),
                np.count_nonzero(maskz1.cpu().numpy() == 1)
            )
            quality = np.count_nonzero(
                threed[maskz1 == 1].cpu().numpy() == label_batch1[maskz1 == 1].cpu().numpy()
            ) / np.count_nonzero(maskz1.cpu().numpy() == 1) #3D输出和正交标签之间准确率
            if quality>args.quality_bar:
                threedmask1=confidence3d > softthreedthreshold
                threedmask2=confidence3d > softthreedthreshold

            ## calculate the loss
            label_batch1=label_batch1.long()
            twod1[maskz1==1]=label_batch1[maskz1==1]
            twodmask=consistency_weight*twodmask
            twodmask[maskz1==1]=1
            loss_seg1 = losses.wce(outputs1, twod1, twodmask, batch_size, patch_size[0], patch_size[1],
                                      patch_size[2])
            loss_seg_dice1=losses.multi_dice_loss_weight(outputs_soft1,twod1,twodmask,classnum=1)
            supervised_loss1 = 0.5 * (loss_seg1 + loss_seg_dice1)
            #print(loss_seg1.item(), loss_seg_dice1.item()) nan nan
            threed[maskz1 == 1] = label_batch1[maskz1 == 1]
            threedmask1 = consistency_weight * threedmask1
            threedmask1[maskz1 == 1] = 1 #正交标注位置，掩码为1，其他位置要乘以consistency_weight
            loss_seg2 = losses.wce(outputs2, threed, threedmask1, batch_size, patch_size[0], patch_size[1],
                                   patch_size[2])
            loss_seg_dice2= losses.multi_dice_loss_weight(outputs_soft2, threed, threedmask1, classnum=1)
            supervised_loss2 = 0.5 * (loss_seg2 + loss_seg_dice2)
            threedmask2 = consistency_weight * threedmask2
            threedmask2[maskz1 == 1] = 1
            loss_seg3 = losses.wce(outputs3, threed, threedmask2, batch_size, patch_size[0], patch_size[1],
                                   patch_size[2])
            loss_seg_dice3= losses.multi_dice_loss_weight(outputs_soft3, threed, threedmask2, classnum=1)
            supervised_loss3 = 0.5 * (loss_seg3 + loss_seg_dice3)
            # total loss
            loss = supervised_loss1+supervised_loss2+supervised_loss3

            #loss=loss_seg_dice1+loss_seg_dice2
            optimizer1.zero_grad()
            optimizer2.zero_grad()
            optimizer3.zero_grad()
            loss.backward()
            optimizer1.step()
            optimizer2.step()
            optimizer3.step()

            iter_num = iter_num + 1
            logging.info('iteration %d : 3d loss : %f 2d loss : %f, %f, mask num %d %d %d, quality %f ' %(iter_num,supervised_loss1.item(),supervised_loss2.item(),supervised_loss3.item(),torch.count_nonzero(twodmask).item(),torch.count_nonzero(threedmask1).item(),torch.count_nonzero(threedmask2).item(),quality))
            if iter_num >= max_iterations:
                break
            time1 = time.time()
        if iter_num >= max_iterations:
            break

    save_mode_path = os.path.join(snapshot_path, 'model.pth')
    torch.save(model1.module.state_dict(), save_mode_path)
    logging.info("save model to {}".format(save_mode_path))
