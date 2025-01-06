package com.zoombit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zoombit.domain.MarketInfo;
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
    void BithumbApiTest() throws JsonProcessingException {
        RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
        String url = "https://api.bithumb.com/v1/market/all?isDetails=false";
        String response = restTemplate.getForObject(url, String.class);
        ObjectMapper objectMapper = new ObjectMapper();
        List<MarketInfo> marketInfoList = objectMapper.readValue(response, new TypeReference<List<MarketInfo>>() {});

        // 결과 출력
        System.out.println("##### marketInfoList.size(): " + marketInfoList.size());
        int idx = 1;
        for (MarketInfo marketInfo : marketInfoList) {
            System.out.println("idx: " + idx + " == " + marketInfo);
            idx++;
        }
    }

}
