package com.zoombit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zoombit.dto.TickerDTO;
import com.zoombit.dto.MarketDTO;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

@SpringBootTest
@ActiveProfiles("test")
class ZoomBitApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void getMarketsTest() throws JsonProcessingException {
        RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
        String url = "https://api.bithumb.com/v1/market/all?isDetails=false";
        String response = restTemplate.getForObject(url, String.class);
        ObjectMapper objectMapper = new ObjectMapper();
        List<MarketDTO> marketDtos = objectMapper.readValue(response, new TypeReference<List<MarketDTO>>() {});

        // 결과 출력
        System.out.println("##### marketDtos.size(): " + marketDtos.size());
        int idx = 1;
        for (MarketDTO marketDto : marketDtos) {
            System.out.println("idx: " + idx + " == " + marketDto);
            idx++;
        }
    }

    @Test
    void getCurrentPriceTest() throws JsonProcessingException {
        RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
        String url = "https://api.bithumb.com/v1/ticker?markets=KRW-BTC";
        String response = restTemplate.getForObject(url, String.class);
        ObjectMapper objectMapper = new ObjectMapper();
        List<TickerDTO> currentPriceList = objectMapper.readValue(response, new TypeReference<List<TickerDTO>>() {});

        // 첫 번째 객체 출력
        TickerDTO currentPrice = currentPriceList.get(0);
        System.out.println(currentPrice);
        // 결과 출력
        System.out.println("##### CurrentPriceDTO.size(): " + currentPriceList.size());
        int idx = 1;
        for (TickerDTO marketDto : currentPriceList) {
            System.out.println("idx: " + idx + " == " + marketDto);
            idx++;
        }
    }

}
