package com.zoombit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zoombit.domain.Markets;
import com.zoombit.dto.MarketDTO;
import com.zoombit.repository.MarketRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class MarketService {

    public static final Logger logger = LoggerFactory.getLogger(MarketService.class.getName());

    @Autowired
    private MarketRepository marketRepository;

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

}
