package com.suttori.dao;

import com.suttori.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.ArrayList;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    User findByUserId(Long userId);

    ArrayList<User> findAllByReferral(String referral);

    User findUserByChatId(Long chatId);

    User findByUserName(String userName);

    ArrayList<User> findAllByPremiumBotUserIsNull();

    ArrayList<User> findAllByLastActivityAfter(Timestamp date);


    @Transactional
    @Modifying
    @Query(value = "UPDATE \"user\" SET current_manga_catalog = ? WHERE user_id = ?", nativeQuery = true)
    void setCurrentMangaCatalog(@Param("current_manga_catalog") String current_manga_catalog, @Param("user_id") Long user_id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE \"user\" SET last_activity = ? WHERE user_id = ?", nativeQuery = true)
    void setLastActivity(@Param("last_activity") Timestamp last_activity, @Param("user_id") Long user_id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE \"user\" SET position = ? WHERE user_id = ?", nativeQuery = true)
    void setPosition(@Param("position") String position, @Param("user_id") Long user_id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE \"user\" SET current_page_in_sticker_set_storage = ? WHERE user_id = ?", nativeQuery = true)
    void setCurrentPageInStickerSetStorage(@Param("current_page_in_sticker_set_storage") int current_page_in_sticker_set_storage, @Param("user_id") Long user_id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE \"user\" SET current_sticker_set_address = ? WHERE user_id = ?", nativeQuery = true)
    void setCurrentStickerSetAddress(@Param("current_sticker_set_address") String current_sticker_set_address, @Param("user_id") Long user_id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE \"user\" SET current_set_name = ? WHERE user_id = ?", nativeQuery = true)
    void setCurrentSetName(@Param("current_set_name") String current_set_name, @Param("user_id") Long user_id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE \"user\" SET balance = ? WHERE user_id = ?", nativeQuery = true)
    void setBalance(@Param("balance") int balance, @Param("user_id") Long user_id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE \"user\" SET premium_bot_user = ? WHERE user_name = ?", nativeQuery = true)
    void setPremium(@Param("premium_bot_user") String premium_bot_user, @Param("user_name") String userName);

    @Transactional
    @Modifying
    @Query(value = "UPDATE \"user\" SET access_status = ? WHERE user_id = ?", nativeQuery = true)
    void setAccessStatus(@Param("access_status") boolean access_status, @Param("user_id") Long user_id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE \"user\" SET is_alive = ? WHERE user_id = ?", nativeQuery = true)
    void setIsAlive(@Param("is_alive") boolean is_alive, @Param("user_id") Long user_id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE \"user\" SET temporary_message_id = ? WHERE user_id = ?", nativeQuery = true)
    void setTemporaryMessageId(@Param("temporary_message_id") Long temporary_message_id, @Param("user_id") Long user_id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE \"user\" SET temporary_source_sticker_set_name = ? WHERE user_id = ?", nativeQuery = true)
    void setTemporarySourceStickerSetName(@Param("temporary_source_sticker_set_name") String temporary_source_sticker_set_name, @Param("user_id") Long user_id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE \"user\" SET language_code = ? WHERE user_id = ?", nativeQuery = true)
    void setLocale(@Param("language_code") String language_code, @Param("user_id") Long user_id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE \"user\" SET temporary_file_unique_id = ? WHERE user_id = ?", nativeQuery = true)
    void setTemporaryFileUniqueId(@Param("temporary_file_unique_id") String temporary_file_unique_id, @Param("user_id") Long user_id);

}
