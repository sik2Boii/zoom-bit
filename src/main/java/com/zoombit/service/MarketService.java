package com.zoombit.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zoombit.dto.MarketDTO;
import com.zoombit.domain.Markets;
import com.zoombit.repository.MarketRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class MarketService {

    @Autowired
    private MarketRepository marketRepository;

    public void saveAllMarkets() throws Exception {
        RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
        String url = "https://api.bithumb.com/v1/market/all?isDetails=false";
        String response = restTemplate.getForObject(url, String.class);

        ObjectMapper objectMapper = new ObjectMapper();
        List<MarketDTO> marketDtos = objectMapper.readValue(response, new TypeReference<List<MarketDTO>>() {});

        for (MarketDTO dto : marketDtos) {
            Markets market = new Markets();
            market.setMarket(dto.getMarket());
            market.setKoreanName(dto.getKorean_name());
            market.setEnglishName(dto.getEnglish_name());
            marketRepository.save(market);
        }

        System.out.println("All markets saved successfully!");
    }

}
