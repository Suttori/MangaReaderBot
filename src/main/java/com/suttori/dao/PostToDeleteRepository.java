package com.suttori.dao;

import com.suttori.entity.PostToDelete;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.ArrayList;

@Repository
public interface PostToDeleteRepository extends JpaRepository<PostToDelete, Long> {

    @Transactional
    void deleteAllByPostId(Long postId);

    ArrayList<PostToDelete> findAllByPostId(Long postId);
}
