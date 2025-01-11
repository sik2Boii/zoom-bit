package com.zoombit.controller;

import com.zoombit.service.MarketService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MarketController {

    public static final Logger logger = LoggerFactory.getLogger(MarketController.class.getName());

    @Autowired
    private MarketService marketService;

    @GetMapping("/save-all-markets")
    public ResponseEntity<String> saveMarkets() {

        try {
            marketService.saveAllMarkets();
            return new ResponseEntity<>("모든 마켓 저장 성공", HttpStatus.OK);
        } catch (Exception e) {
            logger.error("모든 마켓 저장 실패", e);
            return new ResponseEntity<>("모든 마켓 저장 실패", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/send-all-ticker")
    public ResponseEntity<String> sendAllTickerMessage() {
        marketService.getAllMarketTicker();
        return ResponseEntity.ok("Market data sent to Kafka successfully");
    }
}
