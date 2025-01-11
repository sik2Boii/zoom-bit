package com.zoombit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zoombit.domain.Markets;
import com.zoombit.dto.CurrentPriceDTO;
import com.zoombit.dto.MarketDTO;
import com.zoombit.repository.MarketRepository;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class MarketService {

    public static final Logger logger = LoggerFactory.getLogger(MarketService.class.getName());

    @Autowired
    private MarketRepository marketRepository;

    private final String API_URL = "https://api.bithumb.com/v1/ticker?markets=";
    private final int BATCH_SIZE = 150; // 초당 최대 호출 횟수
    private final int DELAY_MS = 1500; // 1초 대기

    public void saveAllMarkets() {

        RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
        String url = "https://api.bithumb.com/v1/market/all?isDetails=false";
        String response = restTemplate.getForObject(url, String.class);

        ObjectMapper objectMapper = new ObjectMapper();
        List<MarketDTO> marketDtos = null;

        try {
            marketDtos = objectMapper.readValue(response, new TypeReference<List<MarketDTO>>() {});
        } catch (JsonProcessingException e) {
            logger.error("JSON 처리 중 오류 발생: " + e.getMessage());
            throw new RuntimeException("JSON 데이터 처리 실패", e);
        }

        for (MarketDTO dto : marketDtos) {
            Markets market = new Markets();
            market.setMarket(dto.getMarket());
            market.setKoreanName(dto.getKorean_name());
            market.setEnglishName(dto.getEnglish_name());
            marketRepository.save(market);
        }

    }

    public List<String> getAllMarkets() {
        return marketRepository.findAllMarketIds();
    }

    public ResponseEntity<String> getAllCurrentPrice() {

        List<String> allMarketData = new ArrayList<>();
        RestTemplate restTemplate = new RestTemplate();
        List<String> marketIds = getAllMarkets();

        int totalMarkets = marketIds.size();

        for (int i = 0; i < totalMarkets; i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, totalMarkets);
            List<String> batchMarkets = marketIds.subList(i, end);

            // 배치 내 각 시장 데이터를 호출
            for (String market : batchMarkets) {
                String url = API_URL + market;
                String response = restTemplate.getForObject(url, String.class);
                allMarketData.add(response);
            }

            if (end < totalMarkets) {
                try {
                    Thread.sleep(DELAY_MS);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        // JSON 형태로 데이터를 응답
        String responseData = String.join(", ", allMarketData);
        return ResponseEntity.ok(responseData); // HTTP 200 OK와 함께 데이터 반환
    }
}
