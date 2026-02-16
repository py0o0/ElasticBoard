package com.example.escommunity.service;

import com.example.escommunity.repository.PostRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ViewSynService {

    private final RedisService redisService;
    private final PostRepository postRepository;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void viewSynToDB(){ // redis 조회수 적용
        Map<Long, Long> views = redisService.getAllView();

        for(Map.Entry<Long, Long> entry : views.entrySet()){
            postRepository.findById(entry.getKey()).ifPresent(post -> {
                post.setViews(entry.getValue() + post.getViews());
                redisService.deleteView(entry.getKey());
            });
        }
    }
}
