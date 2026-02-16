package com.example.escommunity.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RedisService {
    private final StringRedisTemplate redisTemplate;

    private static final String VIEW_KEY_PREFIX = "post:view:";

    public void setRefreshToken(String key, String token, long expireMillis) {
        redisTemplate.opsForValue().set(key, token, expireMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    public void deleteRefreshToken(String email) {
        String key = email;
        redisTemplate.delete(key);
    }

    public void incrementView(Long postId){
        String key = VIEW_KEY_PREFIX + postId;
        redisTemplate.opsForValue().increment(key, 1);
    }

    public Map<Long, Long> getAllView(){
        Map<Long, Long> result = new HashMap<>();
        Set<String> keys = redisTemplate.keys(VIEW_KEY_PREFIX + "*");
        if(keys != null && !keys.isEmpty()){
            for(String key : keys){
                long count = Long.parseLong(redisTemplate.opsForValue().get(key));
                long postId = Long.parseLong(key.replace(VIEW_KEY_PREFIX, ""));
                result.put(postId, count);
            }
        }
        return result;
    }

    public void deleteView(long postId){
        redisTemplate.delete(VIEW_KEY_PREFIX + postId);
    }
}
