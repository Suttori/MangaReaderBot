package com.suttori.dao;

import com.suttori.entity.MediaGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedList;

@Repository
public interface MediaGroupRepository extends JpaRepository<MediaGroup, Long> {

    LinkedList<MediaGroup> findMediaGroupByPostId(@Param("post_id") Long postId);

}
