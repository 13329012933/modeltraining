package com.cn.org.modeltraining.commom.hander;


import com.cn.org.modeltraining.service.IFileManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.*;


@Component
public class TrainingWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private IFileManageService iFileManageService;

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
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // 连接建立时的日志
        System.out.println("WebSocket Connected: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
//        // 前端传来的 npyNames 列表
//        String payload = message.getPayload();
//        List<String> npyNames = new Gson().fromJson(payload, new TypeToken<List<String>>(){}.getType());
//
//        if (npyNames == null || npyNames.isEmpty()) {
//            session.sendMessage(new TextMessage("未选择任何npy文件！"));
//            return;
//        }
//        QueryWrapper<FileManage> wrapper = new QueryWrapper<>();
//        wrapper.in("npy_name", npyNames).eq("status", 0);
//        List<FileManage> list = iFileManageService.list(wrapper);
//        if (list == null || list.isEmpty()) {
//            session.sendMessage(new TextMessage("未找到符合条件的文件或已被使用！"));
//            return;
//        }
//        String uuid = UUID.randomUUID().toString();
//        String fileName = "project" + uuid;
//        File yDir = new File(projectPath, fileName + "/data/train/y/");
//        File xDir = new File(projectPath, fileName + "/data/train/x/");
//        yDir.mkdirs();
//        xDir.mkdirs();
//        for (FileManage item : list) {
//            CommonUtils.toCopyFile(item.getGeneraPath(), yDir.getPath());
//            CommonUtils.toCopyFile(item.getSourcePath(), xDir.getPath());
//        }
//        FileManage fileManage = list.get(0);
//        Integer startNum = fileManage.getStartNum() == null ? 0 : fileManage.getStartNum();
//        Integer septalNum = fileManage.getSeptalNum() == null ? 0 : fileManage.getSeptalNum();
//        ParamDTO dto;
//        try (FileReader reader = new FileReader(pathPy + "params.json")) {
//            dto = new Gson().fromJson(reader, ParamDTO.class);
//        } catch (IOException e) {
//            session.sendMessage(new TextMessage("读取参数模板失败：" + e.getMessage()));
//            return;
//        }
//        dto.setExp(fileName);
//        dto.setInit(startNum);
//        dto.setNum(list.size());
//        dto.setSlice_strategy(septalNum);
//
//        File jsonFile = new File(projectPath + fileName, "params.json");
//        try {
//            new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(jsonFile, dto);
//        } catch (IOException e) {
//            session.sendMessage(new TextMessage("写入参数文件失败：" + e.getMessage()));
//            return;
//        }
//        CommonUtils.toCopyFile(pathPy + "code/model.pth", projectPath + fileName);
//        CommonUtils.toCopyFile(pathPy + "code/log.txt", projectPath + fileName);
//        ProcessBuilder processBuilder = new ProcessBuilder(
//                "python", pathPy + "code/train_3d2dTest.py", projectPath + fileName
//        );
//        processBuilder.redirectErrorStream(true);
//        try {
//            Process process = processBuilder.start();
//            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
//                String line;
//                while ((line = reader.readLine()) != null) {
//                    session.sendMessage(new TextMessage("模型训练中：=======" + line));
//                }
//            }
//            int exitCode = process.waitFor();
//            if (exitCode == 0) {
//                // 成功更新状态
//                UpdateWrapper<FileManage> updateWrapper = new UpdateWrapper<>();
//                updateWrapper.set("status", 1).in("npy_name", npyNames);
//                iFileManageService.update(updateWrapper);
//                session.sendMessage(new TextMessage("训练完成"));
//            } else {
//                session.sendMessage(new TextMessage("训练失败，exitCode: " + exitCode));
//            }
        String project = message.getPayload();
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
                session.sendMessage(new TextMessage("模型正在训练============" + line));
                System.out.println(line);
            }
            int exitCode1;
            exitCode1 = process.waitFor();
            if (exitCode1 != 0 ){
                session.sendMessage(new TextMessage("模型训练失败！！！" + line));
            }
            session.sendMessage(new TextMessage("模型训练完成========="));
        } catch (IOException | InterruptedException e) {
            session.sendMessage(new TextMessage("执行异常：" + e.getMessage()));
        } finally {
            session.close();
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        System.err.println("WebSocket Error: " + exception.getMessage());
    }
}
