package com.example.escommunity.repository;

import com.example.escommunity.entity.PostDocument;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface PostDocumentRepository extends ElasticsearchRepository<PostDocument, Long> {


    List<PostDocument> findByTitleContaining(String keyword, Pageable pageable);

    List<PostDocument> findByContentContaining(String keyword, Pageable pageable);

    List<PostDocument> findByTitleContainingOrContentContaining(String keyword, String keyword1, Pageable pageable);
}
