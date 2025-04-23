package com.cn.org.modeltraining.configuration;

import com.cn.org.modeltraining.commom.hander.TrainingWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler(), "/ws/train")  //确保路径一致
                .setAllowedOrigins("*"); //临时允许所有跨域
    }

    @Bean
    public WebSocketHandler webSocketHandler() {
        return new TrainingWebSocketHandler();
    }
}
