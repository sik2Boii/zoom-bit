package com.zoombit.config;

import com.zoombit.domain.User;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest
public class RedisConfigTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void testRedisConnectionAndSerialization() {
        String key = "user1";
        User value = new User("test@test.com", "bitcoin", 10000);

        redisTemplate.opsForValue().set(key, value);

        User retrievedValue = (User) redisTemplate.opsForValue().get(key);

        System.out.println("=====>> ##### JSON Object: " + retrievedValue);

        // 검증
        Assertions.assertThat(retrievedValue).isNotNull();
        Assertions.assertThat(retrievedValue.getEmail()).isEqualTo(value.getEmail());
        Assertions.assertThat(retrievedValue.getMarket()).isEqualTo(value.getMarket());
        Assertions.assertThat(retrievedValue.getTargetPrice()).isEqualTo(value.getTargetPrice());
    }
}
