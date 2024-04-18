package com.suttori.dao;

import com.suttori.entity.FriendEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
@Repository
public interface FriendEntityRepository extends JpaRepository<FriendEntity, Long> {

    ArrayList<FriendEntity> findAllByUserId(Long userId);

    FriendEntity findByUserIdAndFriendId(Long userId, Long friendId);
}
