package com.zoombit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import com.zoombit.domain.Markets;
import com.zoombit.dto.MarketDTO;
import com.zoombit.dto.TickerDTO;
import com.zoombit.repository.MarketRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
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

    @Qualifier("redisTemplate")
    @Autowired
    private RedisTemplate redisTemplate;

    ObjectMapper objectMapper = new ObjectMapper();

    private final String API_MARKET_URL = "https://api.bithumb.com/v1/market/all?isDetails=false";
    private final String API_TICKER_URL = "https://api.bithumb.com/v1/ticker?markets=";
    private final int BATCH_SIZE = 100;
    private final int API_CALL_LIMIT = 100;

    public void saveAllMarkets() {

        RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
        String response = restTemplate.getForObject(API_MARKET_URL, String.class);

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

    @Scheduled(fixedRate = 60000)
    public void sendAllTickersToKafka() {

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

    @KafkaListener(topics = "ticker-topic", groupId = "data-group")
    public void consume(ConsumerRecord<String, String> record) {
        String key = record.key();
        String value = record.value();
        int partition = record.partition();
        long offset = record.offset();

        redisTemplate.opsForValue().set(key, value);
        redisTemplate.expire(key, 60, TimeUnit.SECONDS);
    }

    private List<Map<String, Object>> getTop10ByTradePrice(List<Map<String, Object>> currentData) {
        return null;
    }

    public List<Map<String, Object>> getAllDataFromRedis() {
        // 모든 키 가져오기
        Set<String> keys = redisTemplate.keys("*");

        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList(); // 키가 없으면 빈 리스트 반환
        }

        // 각 키의 값을 조회하여 리스트로 반환
        return keys.stream()
                .map(key -> {
                    String value = (String) redisTemplate.opsForValue().get(key);
                    return parseToMap(key, value);
                })
                .collect(Collectors.toList());
    }

    private Map<String, Object> parseToMap(String key, String value) {
        Map<String, Object> map = new HashMap<>();
        map.put("key", key); // 키를 저장
        map.put("value", value); // 값을 저장
        return map;
    }

    public List<TickerDTO> getTop10ByTradePrice() {

        List<Map<String, Object>> currentData = getAllDataFromRedis();
        List<TickerDTO> top10ByTradePrice = new ArrayList<>();

        if (currentData != null) {

            try {
                List<String> marketIds = getAllMarkets();
                List<TickerDTO> tickerList = new ArrayList<>();

                for (String marketId : marketIds) {
                    String redisData = (String) redisTemplate.opsForValue().get(marketId);
                    System.out.println(redisData);

                    if (redisData.contains("\"error\"")) {
                        logger.warn("Skipping data for marketId {} due to error field", marketId);
                        continue;
                    }

                    List<TickerDTO> tmp = objectMapper.readValue(redisData, new TypeReference<List<TickerDTO>>() {});
                    TickerDTO ticker = tmp.get(0);
                    tickerList.add(ticker);

                }

                // 현재 가격 기준으로 정렬하여 상위 10개 추출
                top10ByTradePrice = tickerList.stream()
                        .sorted((a, b) -> Double.compare(b.getTrade_price(), a.getTrade_price()))
                        .limit(10)
                        .collect(Collectors.toList());

            } catch (Exception e) {
                logger.error("Error while processing top 10 data: {}", e.getMessage());
            }

        }

        return top10ByTradePrice;

    }

}
