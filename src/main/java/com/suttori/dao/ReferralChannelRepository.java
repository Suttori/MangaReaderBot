package com.suttori.dao;

import com.suttori.entity.ReferralChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.ArrayList;

@Repository
public interface ReferralChannelRepository extends JpaRepository<ReferralChannel, Long> {

    ArrayList<ReferralChannel> findAllByEnableChannel(Boolean enableChannel);

    @Transactional
    @Modifying
    @Query(value = "UPDATE channel SET enable_channel = ? WHERE channel_id = ?", nativeQuery = true)
    void setEnableChannel(@Param("enable_channel") boolean enableChannel, @Param("channel_id") Long channel_id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE channel SET set_link = ? WHERE channel_id = ?", nativeQuery = true)
    void setSetLink(@Param("set_link") boolean setLink, @Param("channel_id") Long channel_id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE channel SET bot_token = ? WHERE channel_id = ?", nativeQuery = true)
    void setBotToken(@Param("bot_token") String bot_token, @Param("channel_id") Long channel_id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE channel SET set_link = false WHERE set_link = true", nativeQuery = true)
    void setSetAllLinkFalse();

    @Transactional
    @Modifying
    @Query(value = "UPDATE channel SET link = ? WHERE set_link = ?", nativeQuery = true)
    void setLink(@Param("link") String link, @Param("set_link") boolean setLink);

    @Transactional
    @Modifying
    @Query(value = "UPDATE channel SET add_in_referral = ? WHERE channel_id = ?", nativeQuery = true)
    void setAddInReferralTime(@Param("add_in_referral") Timestamp add_in_referral, @Param("channel_id") Long channel_id);

    ReferralChannel findByChannelId(Long channelId);

    ReferralChannel findBySetLink(Boolean param);


}
