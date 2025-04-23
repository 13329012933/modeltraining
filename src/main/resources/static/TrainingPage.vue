<template>
  <div>
    <el-button @click="startTraining" type="primary">开始训练</el-button>

    <!-- 实时日志展示 -->
    <el-card>
      <div slot="header" class="clearfix">
        <span>训练日志</span>
      </div>
      <el-scrollbar style="height: 400px;">
        <div v-for="(log, index) in logs" :key="index" class="log-line">
          {{ log }}
        </div>
      </el-scrollbar>
    </el-card>
  </div>
</template>

<script>
export default {
  data() {
    return {
      socket: null,       // WebSocket 连接实例
      logs: [],          // 存储实时日志
      isTraining: false, // 训练状态标志
    };
  },
  methods: {
    // 启动训练
    startTraining() {
      if (this.isTraining) return;  // 防止多次点击

      this.isTraining = true;
      this.logs = [];  // 清空日志
      this.socket = new WebSocket("ws://localhost:8080/ws/train"); // 修改为你后端的 WebSocket 地址

      // 监听 WebSocket 连接建立
      this.socket.onopen = () => {
        console.log("WebSocket 连接已建立");
        // 发送训练请求（例如：选择文件或其它参数）
        this.socket.send(JSON.stringify(["file1.npy", "file2.npy"]));  // 传递训练所需的npy文件列表
      };

      // 监听 WebSocket 消息
      this.socket.onmessage = (event) => {
        const logMessage = event.data;
        this.logs.push(logMessage);  // 将新日志添加到日志列表中
        this.$nextTick(() => {
          // 滚动到底部
          const container = this.$el.querySelector('.el-scrollbar__wrap');
          container.scrollTop = container.scrollHeight;
        });
      };

      // 监听 WebSocket 错误
      this.socket.onerror = (error) => {
        console.error("WebSocket 错误", error);
        this.isTraining = false;
      };

      // 监听 WebSocket 关闭
      this.socket.onclose = () => {
        console.log("WebSocket 连接已关闭");
        this.isTraining = false;
      };
    },
  },
  beforeDestroy() {
    if (this.socket) {
      this.socket.close();  // 组件销毁时关闭 WebSocket 连接
    }
  },
};
</script>

<style scoped>
.log-line {
  word-wrap: break-word;
  white-space: pre-wrap;
  margin: 5px 0;
  font-family: monospace;
}
</style>
