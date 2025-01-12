package com.zoombit.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zoombit.dto.TickerDTO;
import com.zoombit.service.MarketService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class TickerWebSocketHandler extends TextWebSocketHandler {

//    private final List<WebSocketSession> sessions = Collections.synchronizedList(new ArrayList<>());
    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    private final  MarketService marketService;
    private final ObjectMapper objectMapper;

    public TickerWebSocketHandler(MarketService marketService, ObjectMapper objectMapper) {
        this.marketService = marketService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        System.out.println("WebSocket 연결 성공: " + session.getId());
        System.out.println("afterConnectionEstablished 현재 활성 세션 수: " + sessions.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        System.out.println("WebSocket connection closed: " + session.getId());
        System.out.println("Closed 현재 활성화된 세션 수: " + sessions.size());
    }


    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        System.out.println("Received message: " + message.getPayload());
//        session.sendMessage(new TextMessage("Message received"));
        System.out.println("handleTextMessage 현재 활성화된 세션 수: " + sessions.size());

        // 메시지에 따라 처리 로직 추가
        if ("getTop10".equals(message.getPayload())) {
            List<TickerDTO> top10List = marketService.getTop10ByTradePrice();
            String payload = objectMapper.writeValueAsString(top10List);
            session.sendMessage(new TextMessage(payload)); // Top 10 데이터 전송
        } else {
            session.sendMessage(new TextMessage("Unknown command"));
        }

    }

    @Scheduled(fixedRate = 20000)
    public void pushTop10Data() throws Exception {
        List<TickerDTO> top10List = marketService.getTop10ByTradePrice();
        String json = objectMapper.writeValueAsString(top10List);

        for (WebSocketSession session : sessions) {
            System.out.println("Session open 상태: " + session.isOpen());
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(json));
                } catch (IOException e) {
                    System.err.println("Failed to send message: " + e.getMessage());
                    sessions.remove(session);
                }
            }
        }
    }


}
