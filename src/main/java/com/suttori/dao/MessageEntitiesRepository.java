package com.suttori.dao;

import com.suttori.entity.MessageFormatting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;

@Repository
public interface MessageEntitiesRepository extends JpaRepository<MessageFormatting, Long> {


    ArrayList<MessageFormatting> findMessageFormattingByPostId(@Param("post_id") Long postId);

}
