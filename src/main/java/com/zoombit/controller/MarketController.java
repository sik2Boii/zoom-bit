package com.zoombit.controller;

import com.zoombit.dto.TickerDTO;
import com.zoombit.service.MarketService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @GetMapping("/api/save-all-markets")
    public ResponseEntity<String> saveMarkets() {
        try {
            marketService.saveAllMarkets();
            return ResponseEntity.ok("All markets saved successfully.");
        } catch (Exception e) {
            logger.error("Failed to save all markets.", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to save all markets.");
        }
    }

    @GetMapping("/api/send-all-ticker")
    public ResponseEntity<String> sendAllTickerMessage() {
        try {
            marketService.sendAllTickersToKafka();
            return ResponseEntity.ok("Tickers sent to Kafka successfully");
        } catch (Exception e) {
            return new ResponseEntity<>("Failed to send tickers to Kafka", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/api/get-top10")
    public ResponseEntity<List<TickerDTO>> getTop10() {
        try {
            List<TickerDTO> top10List = marketService.getTop10ByTradePrice();
            return ResponseEntity.ok(top10List);
        } catch (Exception e) {
            logger.error("Failed to retrieve the top 10 by trade price.", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
