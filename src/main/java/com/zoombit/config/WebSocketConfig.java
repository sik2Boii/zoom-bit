package com.zoombit.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zoombit.handler.TickerWebSocketHandler;
import com.zoombit.service.MarketService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final TickerWebSocketHandler tickerWebSocketHandler;
    private final MarketService marketService;
    private final ObjectMapper objectMapper;



    public WebSocketConfig(TickerWebSocketHandler tickerWebSocketHandler, MarketService marketService, ObjectMapper objectMapper) {
        this.tickerWebSocketHandler = tickerWebSocketHandler;
        this.marketService = marketService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(tickerWebSocketHandler, "/ws/get-top10")
//                .setAllowedOrigins("http://localhost:5173"); // React 프론트엔드 URL
                .setAllowedOrigins("*"); // React 프론트엔드 URL
    }

}
