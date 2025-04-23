package com.cn.org.modeltraining.controller;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.cn.org.modeltraining.commom.Result;
import com.cn.org.modeltraining.entity.FileManage;
import com.cn.org.modeltraining.entity.FileManageTest;
import com.cn.org.modeltraining.entity.ImageLabelDto;
import com.cn.org.modeltraining.entity.ParamDTO;
import com.cn.org.modeltraining.service.IDemoService;
import com.cn.org.modeltraining.service.IFileManageService;
import com.cn.org.modeltraining.service.IFileManageTestService;
import com.cn.org.modeltraining.utils.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author gk
 * @since 2025-03-25
 */
@RestController
@RequestMapping("/demo/training")
public class DemoController {

    @Value("${file.localFileRootWindows}")
    private String fileDir;
    @Value("${file.dirYPathRootWindows}")
    private String uploadDiry;
    @Value("${file.dirXPathRootWindows}") // 配置上传目录
    private String uploadDirx;
    @Value("${file.picPathRootWindows}") // 配置上传目录
    private String picDir;
    @Value("${file.pathRootWindows}")
    private String projectPath;
    @Value("${file.pathRootTest}")
    private String testProjectPath;
    @Value("${file.pathPy}")
    private String pathPy;
    @Autowired
    private IDemoService service;
    @Autowired
    private IFileManageService iFileManageService;
    @Autowired
    private IFileManageTestService fileManageTestService;

//    /**
//     * 上传并解析.npy文件  （本地）
//     * @param file
//     * @return
//     */
//    @PostMapping("/uploadNpyFile")
//    public Result<?> uploadNpy(@RequestParam("file") MultipartFile file) {
//        if (file.isEmpty() || !file.getOriginalFilename().endsWith(".npy")) {
//            return Result.error("只允许上传 .npy 文件");
//        }
//        try {
//            //生成唯一ID作为根目录
//            String uuid = UUID.randomUUID().toString();
//            File rootDir = new File(uploadDir, uuid);
//            rootDir.mkdirs(); // 创建根目录
//            //保存.npy文件到根目录
//            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
//            File savedFile = new File(rootDir, fileName);
//            file.transferTo(savedFile);
//            //运行Python处理脚本，输出到同一个根目录
//            ProcessBuilder processBuilder = new ProcessBuilder(
//                    "python", pathPy + "npy3img.py", savedFile.getAbsolutePath(), "1"
//            );
//            processBuilder.redirectErrorStream(true);
//            Process process = processBuilder.start();
//            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
//                String line;
//                StringBuilder output = new StringBuilder();
//                while ((line = reader.readLine()) != null) {
//                    System.out.println("[Python]: " + line);
//                    output.append(line).append("\n"); // 记录 Python 输出
//                }
//                //等待Python进程执行完成
//                int exitCode = process.waitFor();
//                if (exitCode != 0) {
//                    System.err.println("Python 脚本执行失败，错误输出:\n" + output);
//                    return Result.error("Python 脚本执行失败：" + output);
//                }
//            }
//            String imgPath = rootDir + "/data/" + fileName.replaceAll(".npy","");
//            System.out.println("=================" + imgPath);
//            //读取Python生成的目录
//            File npyFolder = new File(imgPath);
//            if (!npyFolder.exists()) {
//                return Result.error("处理失败，未找到生成文件夹");
//            }
//            List<FileManage> fileManageList = new ArrayList<>();
//            for (String dim : new String[]{"Vertical", "Inline", "Crossline"}) {
//                File dimFolder = new File(npyFolder, dim);
//                if (dimFolder.exists()) {
//                    File[] images = dimFolder.listFiles((dir, name) -> name.endsWith(".png") || name.endsWith(".jpg"));
//                    if (images != null) {
//                        for (File image : images) {
//                            String fileType = image.getName().substring(image.getName().lastIndexOf(".") + 1);
//                            FileManage fileManage = new FileManage();
//                            fileManage.setFileName(image.getName());
//                            fileManage.setFileType(fileType);
//                            fileManage.setFileAddress(image.getAbsolutePath());
//                            fileManage.setNpyName(file.getOriginalFilename());
//                            fileManage.setFileId(Integer.valueOf(image.getName().replaceAll("\\D+", ""))); //生成图片名称中的ID
//                            fileManage.setDimension(dim); //记录图片所属维度
//                            fileManage.setCreateTime(new Date());
//                            fileManageList.add(fileManage);
//                        }
//                    }
//                }
//            }
//            //批量存入数据库
//            if (!fileManageList.isEmpty()) {
//                iFileManageService.saveBatch(fileManageList);
//            }
//            return Result.ok("文件上传成功，图片已生成");
//        } catch (Exception e) {
//            e.printStackTrace();
//            return Result.error("处理文件时发生错误");
//        }
//    }

    /**
     * 上传并解析.npy文件 （minio）
     * @param file
     * @return
     */
    @PostMapping("/uploadNpyFile")
    public Result<?> uploadNpy(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty() || !file.getOriginalFilename().endsWith(".npy")) {
            return Result.error("只允许上传 .npy 文件");
        }
        try {
            File rootDir = new File(uploadDirx);
            File picDirPath = new File(picDir);
            rootDir.mkdirs(); //创建根目录
            picDirPath.mkdirs(); //创建根目录
            //保存.npy文件到根目录
            String fileName = file.getOriginalFilename();
            File savedFile = new File(rootDir, fileName);
            File savePicFile = new File(picDirPath, fileName);
            file.transferTo(savedFile);
            //复制文件到另一个目录
            java.nio.file.Files.copy(savedFile.toPath(), savePicFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            String bizPath = "npy/" + fileName;
            //执行Python脚本处理（传本地路径）
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "python", pathPy + "npy3img.py", savePicFile.getAbsolutePath(), "1"
            );
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[Python]: " + line);
                    output.append(line).append("\n");
                }
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                return Result.error("Python 脚本执行失败：" + output);
            }
            //查找Python生成的图片文件
            String imgPath = picDirPath + "/data/" + fileName.replaceAll(".npy","");
            File imgRootDir = new File(imgPath);
            if (!imgRootDir.exists()) {
                return Result.error("处理失败，未找到生成文件夹");
            }
            List<FileManage> fileManageList = new ArrayList<>();
            for (String dim : new String[]{"Vertical", "Inline", "Crossline"}) {
                File dimFolder = new File(imgRootDir, dim);
                if (dimFolder.exists()) {
                    File[] images = dimFolder.listFiles((dir, name) -> name.endsWith(".png") || name.endsWith(".jpg"));
                    if (images != null) {
                        for (File image : images) {
                            //将图片转为MultipartFile并上传
                            MultipartFile multipartImage = FileToMultipartFileUtil.convert(image);
                            String imagePath = MinioUtil.upload(multipartImage, bizPath + "/data/" + dim, null);
                            FileManage fileManage = new FileManage();
                            fileManage.setFileName(image.getName());
                            fileManage.setFileType(image.getName().substring(image.getName().lastIndexOf(".") + 1));
                            fileManage.setFileAddress(image.getAbsolutePath()); //本地地址
                            fileManage.setMinioUrl(imagePath); //MinIO地址
                            fileManage.setNpyName(file.getOriginalFilename());
                            fileManage.setFileId(Integer.valueOf(image.getName().replaceAll("\\D+", "")));
                            fileManage.setDimension(dim);
                            fileManage.setSourcePath(savedFile.getAbsolutePath());
                            fileManage.setCreateTime(new Date());
                            fileManageList.add(fileManage);
                        }
                    }
                }
            }
            if (!fileManageList.isEmpty()) {
                boolean b = iFileManageService.saveBatch(fileManageList);
                if (b){
                    FolderDeleteUtil.deleteSpecificSubfolder(uploadDirx,"data");
                }
            }
            return Result.ok("文件上传成功，图片已生成");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("处理文件时发生错误：" + e.getMessage());
        }
    }

    /**
     * 查询npy图片列表
     * @return
     */
    @PostMapping("/getNpyList")
    public Result<?> getNpyList(@RequestBody FileManage fileManage) {
        LambdaQueryWrapper<FileManage> wrapper = new LambdaQueryWrapper<>();
        if (fileManage.getNpyName() != null){
            wrapper.like(FileManage::getFileName,fileManage.getNpyName());
        }
        if (fileManage.getCreateTime() != null){
            wrapper.like(FileManage::getCreateTime,fileManage.getCreateTime());
        }
        List<FileManage> list = iFileManageService.list(wrapper);
        //查询数据库
        Set<String> unlabeled = list.stream().filter(f-> f.getStatus() == 0 && f.getBzStatus() == 0).map(FileManage::getNpyName).collect(Collectors.toSet());
        Set<String> npyNames = list.stream().filter(f-> f.getStatus() == 0 && f.getBzStatus() == 1).map(FileManage::getNpyName).collect(Collectors.toSet());
        Set<String> marked = list.stream().filter(f-> f.getStatus() == 1 ).map(FileManage::getNpyName).collect(Collectors.toSet());
        JSONObject res = new JSONObject();
        res.put("unlabeleds",unlabeled);
        res.put("marked",marked);
        res.put("npyNames",npyNames);
        return Result.ok(res);
    }

    /**
     * 查询npy图片详情
     * @return
     */
    @PostMapping("/getNpyDetail")
    public Result<?> getNpyDetail(@RequestBody FileManage fileManage) {
        QueryWrapper<FileManage> wrapper = new QueryWrapper<>();
        wrapper.like("npy_name",fileManage.getNpyName());
        //查询数据库
        List<FileManage> fileList = iFileManageService.list(wrapper);
        Map<String, List<Map<String, Object>>> resultMap = new HashMap<>();
        //根据维度分组
        fileList.forEach(file -> {
            //获取dimension
            String dimension = file.getDimension();
            //获取文件相关信息
            Map<String, Object> fileInfo = new HashMap<>();
            fileInfo.put("id", file.getId());
            fileInfo.put("fileId", file.getFileId());
            fileInfo.put("fileName", file.getFileName());
            fileInfo.put("fileType", file.getFileType());
            fileInfo.put("createTime", file.getCreateTime());
            fileInfo.put("fileAddress", file.getFileAddress());
            fileInfo.put("mimio", file.getMinioUrl());
            fileInfo.put("npyName", file.getNpyName());
            fileInfo.put("dimension", dimension);
            fileInfo.put("labelName", file.getLabelName());
            fileInfo.put("bzStatus", file.getBzStatus());
            fileInfo.put("sourcePath", file.getSourcePath());
            fileInfo.put("generaPath", file.getGeneraPath());
            fileInfo.put("projectName", file.getProjectName());
            fileInfo.put("status", file.getStatus());
            fileInfo.put("septalNum", file.getSeptalNum());
            fileInfo.put("startNum", file.getStartNum());
            fileInfo.put("pointData", file.getPointData());
            //如果该维度下没有图片列表，初始化它
            resultMap.putIfAbsent(dimension, new ArrayList<>());
            //添加当前文件信息到对应维度下的文件列表
            resultMap.get(dimension).add(fileInfo);
        });
        Map<String, Object> result = new HashMap<>();
        result.put("npyName", fileManage.getNpyName());
        result.put("data", resultMap);
        return Result.ok(result);
    }

    /**
     * 图片训练
     * @return
     */
    @PostMapping("/getNpyTestDetail")
    public Result<?> getNpyTestDetail(@RequestBody FileManage fileManage) {
        QueryWrapper<FileManage> wrapper = new QueryWrapper<>();
        wrapper.like("npy_name",fileManage.getNpyName());
        //查询数据库
        List<FileManage> fileList = iFileManageService.list(wrapper);
        Map<String, List<Map<String, Object>>> resultMap = new HashMap<>();
        //根据维度分组
        fileList.forEach(file -> {
            //获取dimension
            String dimension = file.getDimension();
            //获取文件相关信息
            Map<String, Object> fileInfo = new HashMap<>();
            fileInfo.put("id", file.getId());
            fileInfo.put("fileId", file.getFileId());
            fileInfo.put("fileName", file.getFileName());
            fileInfo.put("fileType", file.getFileType());
            fileInfo.put("createTime", file.getCreateTime());
            fileInfo.put("fileAddress", file.getFileAddress());
            fileInfo.put("mimio", file.getMinioUrl());
            fileInfo.put("npyName", file.getNpyName());
            fileInfo.put("dimension", dimension);
            //如果该维度下没有图片列表，初始化它
            resultMap.putIfAbsent(dimension, new ArrayList<>());
            //添加当前文件信息到对应维度下的文件列表
            resultMap.get(dimension).add(fileInfo);
        });
        Map<String, Object> result = new HashMap<>();
        result.put("npyName", fileManage.getNpyName());
        result.put("data", resultMap);
        return Result.ok(result);
    }
    /**
     *  选择照片的取样间隔，展示列表
     * @param npyName
     * @param dimension
     * @param start_num
     * @param septal_num
     * @return
     */
    @GetMapping("/getImagesBySlice")
    public Result<?> getImagesBySlice(
            @RequestParam("npyName") String npyName,
            @RequestParam("dimension") String dimension,
            @RequestParam("start_num") int start_num,
            @RequestParam("septal_num") int septal_num) {
        //查询符合条件的所有图片
        List<FileManage> allImages = iFileManageService.list(
                new QueryWrapper<FileManage>()
                        .eq("npy_name", npyName)
                        .eq("dimension", dimension)
                        .orderByAsc("file_id")
        );
        //计算符合条件的切片索引
        List<FileManage> selectedImages = new ArrayList<>();
        //最大索引
        int maxIndex = allImages.size() - 1;
        if (start_num >= maxIndex) {
            start_num = maxIndex;
            septal_num = 0;
        }
        for (int i = start_num; i <= maxIndex; i += septal_num) {
            selectedImages.add(allImages.get(i));
        }
        List<FileManage> list = iFileManageService.list();
        int finalStart_num = start_num;
        int finalSeptal_num = septal_num;
        list.forEach(f->{
            f.setStartNum(finalStart_num);
            f.setSeptalNum(finalSeptal_num);
        });
        iFileManageService.updateBatchById(list);
        return Result.ok(selectedImages);
    }

    /**
     *  根据id查询
     * @param id
     * @return
     */
    @GetMapping("/getImageById")
    public Result<?> getImageById(@RequestParam("id") Integer id) {
        FileManage img = iFileManageService.getById(id);
        return Result.ok(img);
    }

    /**
     * 提交选择的图片采样
     * @param labelDto
     * @return
     */
    @PostMapping("/submitLabel")
    public Result<?> submitLabel(@RequestBody ImageLabelDto labelDto) {
        FileManage fileManage = new FileManage();
        fileManage.setNpyName(labelDto.getNpyName());
        fileManage.setId(labelDto.getId());
        fileManage.setDimension(labelDto.getDimension());
        fileManage.setFileAddress(labelDto.getFileAddress());
        fileManage.setLabelName(labelDto.getLabelName());
        fileManage.setPointData(JSON.toJSONString(labelDto.getPoints()));
        iFileManageService.updateById(fileManage);
        return Result.ok("标注数据提交成功");
    }

    /**
     * 更新标注状态
     * @param npyName
     * @param dimension
     * @return
     */
    @GetMapping("/updBzStatus")
    public Result<?> updBzStatus(@RequestParam("npyName") String npyName,
                                 @RequestParam("dimension") String dimension) {
        if (StringUtils.isEmpty(npyName) || StringUtils.isEmpty(dimension)){
            return Result.error("参数不能为空");
        }
        UpdateWrapper<FileManage> updWrapper = new UpdateWrapper<>();
        updWrapper.eq("dimension",dimension);
        updWrapper.eq("npy_name",npyName);
        updWrapper.set("bz_status",1);
        boolean update = iFileManageService.update(updWrapper);
        if (update){
            return Result.ok("标注状态更新成功");
        }else {
            return Result.error("标注状态更新失败");
        }

    }

    /**
     * 查询某张图片的所有标注信息
     * @param fileAddress
     * @return
     */
    @GetMapping("/getLabels")
    public Result<?> getLabels(@RequestParam("fileAddress") String fileAddress) {
        List<FileManage> labels = iFileManageService.list(
                new QueryWrapper<FileManage>().eq("file_address", fileAddress)
        );
        List<Map<String, Object>> response = labels.stream().map(label -> {
            Map<String, Object> map = new HashMap<>();
            map.put("labelName", label.getLabelName());
            map.put("points", JSON.parseArray(label.getPointData(), List.class));
            return map;
        }).collect(Collectors.toList());
        return Result.ok(response);
    }

    /**
     *  将维度的采样数据转为npy文件
     * @param npyName
     * @param dim1
     * @param dim2  Model training
     * @return
     */
    @GetMapping("/combineTwoDimensions")
    public Result<?> combineTwoDimensions(@RequestParam("npyName") String npyName,
                                          @RequestParam("dim1") String dim1,
                                          @RequestParam("dim2") String dim2) {
        try {
            List<FileManage> list = iFileManageService.list();
            //获取两个维度的图片列表
            List<FileManage> dim1Files = list.stream()
                    .filter(item -> npyName.equals(item.getNpyName()) && dim1.equals(item.getDimension()))
                    .sorted(Comparator.comparing(FileManage::getFileId))
                    .collect(Collectors.toList());
            List<FileManage> dim2Files = list.stream()
                    .filter(item -> npyName.equals(item.getNpyName()) && dim2.equals(item.getDimension()))
                    .sorted(Comparator.comparing(FileManage::getFileId))
                    .collect(Collectors.toList());
            //调用Python处理npy文件
            int[][][] ints1 = processPythonArray(dim1Files);
            int[][][] ints2 = processPythonArray(dim2Files);
            //将两个数组转置
            int[][][] ints3 = Array3DUtils.transposeToVerticalInlineCrossline(ints1, dim1);
            int[][][] ints4 = Array3DUtils.transposeToVerticalInlineCrossline(ints2, dim2);
            //对两个三维数组进行“或”逻辑运算
            int[][][] combinedArray = combineArrays(ints3, ints4);
            String fileName = npyName;
            //生成.npy文件路径
            File outputFilePath = new File(uploadDiry);
            String fileName1 = fileName.substring(0,fileName.lastIndexOf("."));
            //调用Python保存npy文件
            saveNpyToFile(combinedArray,outputFilePath.getAbsolutePath(),fileName1);
            List<FileManage> fileManages = list.stream()
                    .filter(item -> npyName.equals(item.getNpyName()))
                    .collect(Collectors.toList());
            fileManages.forEach(f-> {
                f.setGeneraPath(outputFilePath.getPath() + "\\" + fileName );
            });
            iFileManageService.updateBatchById(fileManages);
            return Result.ok("生成.npy文件成功，地址：" + outputFilePath.toString());
        } catch (Exception e) {
            return Result.error("合并过程中发生错误：" + e.getMessage());
        }
    }

    /**
     * @param npyNames
     * @return
     */
    @PostMapping("/submitTraining")
    public Result<?> submitTraining(@RequestBody List<String> npyNames) {
        QueryWrapper<FileManage> wrapper = new QueryWrapper<>();
        if (!npyNames.isEmpty()){
            wrapper.in("npy_name",npyNames);
            wrapper.eq("status",0);
        }
        List<FileManage> list = iFileManageService.list(wrapper);
        //生成唯一ID作为根目录
        String uuid = UUID.randomUUID().toString();
        String fileName = "project" + uuid;
        //构造最终完整路径
        File yDir = new File(projectPath, fileName + "/data/train/y/");
        //一次性创建所有目录（包括中间层级）
        yDir.mkdirs();
        File xDir = new File(projectPath, fileName + "/data/train/x/");
        xDir.mkdirs();
        list.forEach(item ->{
            CommonUtils.toCopyFile(item.getGeneraPath(), yDir.getPath());
            CommonUtils.toCopyFile(item.getSourcePath(), xDir.getPath());
        });
        int num = list.size();
        FileManage fileManage = list.get(0);
        Integer startNum = fileManage.getStartNum();
        Integer septalNum = fileManage.getSeptalNum();
        Gson gson = new Gson();
        FileReader reader = null;
        try {
            reader = new FileReader(pathPy + "params.json");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        ParamDTO dto = gson.fromJson(reader, ParamDTO.class);
        dto.setExp(fileName);
        dto.setInit(startNum);
        dto.setNum(num);
        dto.setSlice_strategy(septalNum);
        //指定JSON文件完整路径
        String jsonFilePath = projectPath + fileName;
        File jsonFile = new File(jsonFilePath,"params.json");
        // 写入JSON文件
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(jsonFile, dto);
        } catch (IOException e) {
            e.printStackTrace();
        }
        CommonUtils.toCopyFile(pathPy + "code/model.pth", jsonFilePath);
        CommonUtils.toCopyFile(pathPy + "code/log.txt", jsonFilePath);
        //调用Python训练模型
        ProcessBuilder processBuilder = new ProcessBuilder(
                "python", pathPy + "code/train_3d2d.py", jsonFilePath
        );
        String line;
        try {
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            // 读取输出
            BufferedReader reade = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((line = reade.readLine()) != null) {
                System.out.println(line);
            }
            int exitCode1 = 0;
            exitCode1 = process.waitFor();
            if (exitCode1 != 0 ){
                return Result.OK("生成失败。" );
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        UpdateWrapper<FileManage> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("status",1);
        updateWrapper.in("npy_name",npyNames);
        boolean update = iFileManageService.update(updateWrapper);
        if (update){
            return Result.OK("模型训练正在训练：======" + line);
        }else {
            return Result.error("模型训练失败！");
        }
    }

    /**
     * @param project
     * @return
     */
    @GetMapping("/submitTrainingTest")
    public Result<?> submitTrainingTest(@RequestParam("project") String project) {
        String npyPath = testProjectPath + project;
        //调用Python训练模型
        ProcessBuilder processBuilder = new ProcessBuilder(
                "python", pathPy + "code/train_3d2d.py", npyPath);
        String line;
        try {
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            //读取输出
            BufferedReader reade = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((line = reade.readLine()) != null) {
                System.out.println(line);
            }
            int exitCode1 = 0;
            exitCode1 = process.waitFor();
            if (exitCode1 != 0 ){
                return Result.OK("模型训练失败！！！" + line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return Result.OK("模型训练正在训练：======" + line);
    }

    /**
     * @param npyName
     * @return
     */
    @GetMapping("/modelTest")
    public Result<?> modelTest(@RequestParam("npyName") String npyName) {
        QueryWrapper<FileManageTest> wrapper = new QueryWrapper<>();
        wrapper.eq("npy_name",npyName);
        List<FileManageTest> list = fileManageTestService.list(wrapper);
        if (list.size() > 0){
            return Result.ok(list);
        }
        ProcessBuilder pb = new ProcessBuilder(
                "python",
                pathPy + "code/ceshi.py",
                "--savemodel_path", projectPath + "data",
                "--model_path", pathPy + "code/model.pth",
                "--savedata_path", uploadDirx,
                    "--npy_path", npyName,
                "--output_path", uploadDiry,
                "--num_classes", String.valueOf(2)
        );
        pb.redirectErrorStream(true);
        try {
            Process process = pb.start();
            // 读取输出
            BufferedReader reade = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reade.readLine()) != null) {
                System.out.println(line);
            }
            int exitCode = process.waitFor();
            if (exitCode != 0 ){
                return Result.OK("生成失败。" );
            }
            File rootDir = new File(uploadDiry);
            File picDirPath = new File(picDir);
            rootDir.mkdirs(); //创建根目录
            picDirPath.mkdirs(); //创建根目录
            //保存.npy文件到根目录
            String fileName = npyName;
            File savedFile = new File(rootDir, fileName);
            File savePicFile = new File(picDirPath, fileName);
            //复制文件到另一个目录
            java.nio.file.Files.copy(savedFile.toPath(), savePicFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            //执行Python脚本处理（传本地路径）
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "python", pathPy + "npy3img.py", savePicFile.getAbsolutePath(), "1"
            );
            Process process1 = processBuilder.start();
            int exitCode1 = process1.waitFor();
            if (exitCode1 != 0 ){
                return Result.OK("生成失败。" );
            }
            //查找Python生成的图片文件
            String imgPath = picDirPath + "/data/" + npyName.replaceAll(".npy","");
            File imgRootDir = new File(imgPath);
            if (!imgRootDir.exists()) {
                return Result.error("处理失败，未找到生成文件夹");
            }
            String bizPath = "npy/" + npyName;
            List<FileManageTest> fileManageList = new ArrayList<>();
            for (String dim : new String[]{"Vertical", "Inline", "Crossline"}) {
                File dimFolder = new File(imgRootDir, dim);
                if (dimFolder.exists()) {
                    File[] images = dimFolder.listFiles((dir, name) -> name.endsWith(".png") || name.endsWith(".jpg"));
                    if (images != null) {
                        for (File image : images) {
                            //将图片转为MultipartFile并上传
                            MultipartFile multipartImage = FileToMultipartFileUtil.convert(image);
                            String imagePath = MinioUtil.upload(multipartImage, bizPath + "/data/" + dim, null);
                            FileManageTest fileManage = new FileManageTest();
                            fileManage.setFileName(image.getName());
                            fileManage.setFileType(image.getName().substring(image.getName().lastIndexOf(".") + 1));
                            fileManage.setFileAddress(image.getAbsolutePath()); //本地地址
                            fileManage.setMinioUrl(imagePath); //MinIO地址
                            fileManage.setNpyName(npyName);
                            fileManage.setFileId(Integer.valueOf(image.getName().replaceAll("\\D+", "")));
                            fileManage.setDimension(dim);
                            fileManage.setSourcePath(uploadDiry + npyName);
                            fileManage.setGeneraPath(uploadDirx + npyName);
                            fileManage.setCreateTime(new Date());
                            fileManageList.add(fileManage);
                        }
                    }
                }
            }
            if (!fileManageList.isEmpty()) {
                boolean b = fileManageTestService.saveBatch(fileManageList);
                if (b){
                    list = fileManageTestService.list(wrapper);
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return Result.OK("模型测试成功",list);
    }
    /**
     * 将图片的集合转为三维数组
     * @param fileList
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    private int[][][] processPythonArray(List<FileManage> fileList) throws IOException, InterruptedException {
        Map<Integer, int[][]> fileDataMap = new TreeMap<>();
        Gson gson = new Gson();
        for (FileManage file : fileList) {
            int[][] parsedArray = null;
            if (StringUtils.isNotEmpty(file.getPointData())) {
                //解析JSON
                String image_path = file.getFileAddress();
                //转换成JSON
                String polygons = gson.toJson(file.getPointData());
                //执行Python 脚本
                ProcessBuilder pb = new ProcessBuilder("python", pathPy + "test6.py",
                        pathPy + "lab_temp.json",
                        image_path, polygons);
                pb.redirectErrorStream(true);
                Process process = pb.start();
                //读取Python返回的数据
                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("[Python]: " + line);
                        output.append(line).append("\n");
                    }
                }
                //等待Python进程完成
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    System.err.println("Python 脚本执行失败，错误输出:\n" + output);
                    continue;
                }
                //解析JSON
                String jsonData = output.toString();
                try {
                    JsonObject asJsonObject = JsonParser.parseString(jsonData).getAsJsonObject();
                    if (asJsonObject.has("mask_image")) {
                        JsonElement maskElement = asJsonObject.get("mask_image");
                        if (maskElement.isJsonArray()) {
                            parsedArray = gson.fromJson(maskElement, int[][].class);
                        } else {
                            System.err.println("JSON解析错误: mask_image不是二维数组");
                        }
                    } else {
                        System.err.println("JSON解析错误: JSON数据中没有mask_image字段");
                    }
                } catch (Exception e) {
                    System.err.println("JSON解析失败: " + e.getMessage());
                }
            } else {
                //读取原图片
                File inputFile = new File(file.getFileAddress());
                BufferedImage image = ImageIO.read(inputFile);
                //获取图片宽高
                int width = image.getWidth();
                int height = image.getHeight();
                //生成全0的二维数组
                parsedArray = generateZeroArray(height, width);
            }
            fileDataMap.put(file.getFileId(), parsedArray);
        }
        //将Map按fileId顺序转换为三维数组
        return fileDataMap.values().toArray(new int[0][][]);
    }

    //生成指定大小的全0数组
    public static int[][] generateZeroArray(int height, int width) {
        return new int[height][width]; //默认int值为0
    }

    /**
     *  合并三维数组
     * @param array1
     * @param array2
     * @return
     */
    // 合并函数：按位或
    public static int[][][] combineArrays(int[][][] array1, int[][][] array2) {
        int x = array1.length;
        int y = array1[0].length;
        int z = array1[0][0].length;
        int[][][] result = new int[x][y][z];
        for (int i = 0; i < x; i++) {
            for (int j = 0; j < y; j++) {
                for (int k = 0; k < z; k++) {
                    result[i][j][k] = array1[i][j][k] | array2[i][j][k];
                }
            }
        }
        return result;
    }

    //调用Python保存npy
    private void saveNpyToFile(int[][][] array, String outputPath, String fileName) throws IOException, InterruptedException {
        String jsonPath = picDir + File.separator + fileName + ".json";
        try (JsonWriter writer = new JsonWriter(new FileWriter(jsonPath))) {
            writer.setIndent("  ");
            writer.beginArray(); // [
            for (int[][] twoD : array) {
                writer.beginArray(); // [
                for (int[] oneD : twoD) {
                    writer.beginArray(); // [
                    for (int val : oneD) {
                        writer.value(val);
                    }
                    writer.endArray(); // ]
                }
                writer.endArray(); // ]
            }
            writer.endArray(); // ]
            System.out.println("三维数组已流式写入JSON文件: " + jsonPath);
        } catch (IOException e) {
            throw new IOException("写入JSON文件失败: " + e.getMessage(), e);
        }
        ProcessBuilder pb = new ProcessBuilder("python", pathPy + "save_npy.py", jsonPath, outputPath, fileName);
        Process process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Python脚本执行失败！");
        }
    }

}
