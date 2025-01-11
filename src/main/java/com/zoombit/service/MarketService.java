package com.zoombit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import com.zoombit.domain.Markets;
import com.zoombit.dto.MarketDTO;
import com.zoombit.repository.MarketRepository;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
public class MarketService {

    public static final Logger logger = LoggerFactory.getLogger(MarketService.class.getName());

    @Autowired
    private MarketRepository marketRepository;

    @Qualifier("kafkaTemplate")
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private final String API_MARKET_URL = "https://api.bithumb.com/v1/market/all?isDetails=false";
    private final String API_TICKER_URL = "https://api.bithumb.com/v1/ticker?markets=";
    private final int BATCH_SIZE = 100;
    private final int API_CALL_LIMIT = 100;

    public void saveAllMarkets() {

        RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
        String response = restTemplate.getForObject(API_MARKET_URL, String.class);

        ObjectMapper objectMapper = new ObjectMapper();
        List<MarketDTO> marketDtos = null;

        final KafkaTemplate<String, String> kafkaTemplate;

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

    public void getAllMarketTicker() {

        List<String> marketIds = getAllMarkets();
        RestTemplate restTemplate = new RestTemplate();

        String topicName = "ticker-topic";
        int totalMarketCount = marketIds.size();
        boolean sync = false;
        RateLimiter rateLimiter = RateLimiter.create(API_CALL_LIMIT);

        for (int i = 0; i < totalMarketCount; i += BATCH_SIZE) {

            int end = Math.min(i + BATCH_SIZE, totalMarketCount);
            List<String> batchMarkets = marketIds.subList(i, end);

            for (String market : batchMarkets) {

                rateLimiter.acquire(); // 호출 속도 제한
                String url = API_TICKER_URL + market;

                try {

                    String response = restTemplate.getForObject(url, String.class); // API 호출
                    ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topicName, market, response);
                    HashMap<String, String> messageMap = new HashMap<>();
                    messageMap.put(market, response);

                    sendMessage(kafkaTemplate, producerRecord, messageMap, sync);
                } catch (HttpClientErrorException e) {
                    logger.error("현재가(Ticker) 조회 실패: {}", market, e);
                }

            }

        }

    }

    public void sendMessage(KafkaTemplate<String, String> kafkaTemplate, ProducerRecord<String, String> producerRecord, HashMap<String, String> messageMap, boolean sync) {

        String topic = producerRecord.topic();
        String key = producerRecord.key();
        String value = producerRecord.value();

        if (!sync) {
            // 비동기 전송
            kafkaTemplate.send(topic, key, value)
                    .whenComplete((result, exception) -> {
                        if (exception == null) {
                            logger.info("비동기 - Key: {}, Partition: {}, Offset: {}",
                                    key, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                        } else {
                            logger.error("Exception error from broker: {}", exception.getMessage(), exception);
                        }
                    });
        } else {
            try {
                // 동기 전송
                var metadata = kafkaTemplate.send(topic, key, value).get().getRecordMetadata();
                logger.info("동기 - Key: {}, Partition: {}, Offset: {}",
                        key, metadata.partition(), metadata.offset());
            } catch (ExecutionException e) {
                logger.error("Error occurred while sending message: {}", e.getMessage(), e);
            } catch (InterruptedException e) {
                logger.error("Message sending was interrupted: {}", e.getMessage(), e);
            }
        }
    }

}
