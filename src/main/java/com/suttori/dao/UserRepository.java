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

    ArrayList<User> findAllByIsPremiumBotUserIsNull();

    ArrayList<User> findAllByLastActivityAfter(Timestamp date);

    @Transactional
    @Modifying
    @Query(value = "INSERT INTO \"user\" (user_id, chat_id, first_name, last_name, user_name, language_code, is_telegram_premium, register_time, position, referral, access_status, is_alive, balance, last_activity) " +
            "VALUES (:userId, :chatId, :firstName, :lastName, :userName, :languageCode, :isTelegramPremium, CURRENT_TIMESTAMP, 'DEFAULT', NULL, true, true, 0, CURRENT_TIMESTAMP) " +
            "ON CONFLICT (user_id) " +
            "DO UPDATE SET first_name = EXCLUDED.first_name, last_name = EXCLUDED.last_name, user_name = EXCLUDED.user_name, language_code = EXCLUDED.language_code, is_telegram_premium = EXCLUDED.is_telegram_premium, " +
            "chat_id = EXCLUDED.chat_id, access_status = EXCLUDED.access_status, is_alive = EXCLUDED.is_alive, balance = EXCLUDED.balance, last_activity = EXCLUDED.last_activity",
            nativeQuery = true)
    void upsertUser(@Param("userId") Long userId,
                    @Param("chatId") Long chatId,
                    @Param("firstName") String firstName,
                    @Param("lastName") String lastName,
                    @Param("userName") String userName,
                    @Param("languageCode") String languageCode,
                    @Param("isTelegramPremium") Boolean isTelegramPremium);


    @Transactional
    @Modifying
    @Query(value = "UPDATE \"user\" SET current_manga_catalog = ? WHERE user_id = ?", nativeQuery = true)
    void setCurrentMangaCatalog(@Param("current_manga_catalog") String current_manga_catalog, @Param("user_id") Long user_id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE \"user\" SET language_code_for_catalog = ? WHERE user_id = ?", nativeQuery = true)
    void setCurrentLanguageCodeForCatalog(@Param("language_code_for_catalog") String language_code_for_catalog, @Param("user_id") Long user_id);

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
    @Query(value = "UPDATE \"user\" SET sort_param = ? WHERE user_id = ?", nativeQuery = true)
    void setSortParam(@Param("sort_param") String sort_param, @Param("user_id") Long user_id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE \"user\" SET temporary_message_id = ? WHERE user_id = ?", nativeQuery = true)
    void setTemporaryMessageId(@Param("temporary_message_id") String temporary_message_id, @Param("user_id") Long user_id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE \"user\" SET balance = ? WHERE user_id = ?", nativeQuery = true)
    void setBalance(@Param("balance") int balance, @Param("user_id") Long user_id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE \"user\" SET is_premium_bot_user = ? WHERE user_name = ?", nativeQuery = true)
    void setPremium(@Param("is_premium_bot_user") Boolean is_premium_bot_user, @Param("user_name") String userName);

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
    @Query(value = "UPDATE \"user\" SET language_code = ? WHERE user_id = ?", nativeQuery = true)
    void setLocale(@Param("language_code") String language_code, @Param("user_id") Long user_id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE \"user\" SET manga_format_parameter = ? WHERE user_id = ?", nativeQuery = true)
    void setMangaFormat(@Param("manga_format_parameter") String manga_format_parameter, @Param("user_id") Long user_id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE \"user\" SET number_of_chapters_sent = ? WHERE user_id = ?", nativeQuery = true)
    void setNumberOfChaptersSent(@Param("number_of_chapters_sent") String number_of_chapters_sent, @Param("user_id") Long user_id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE \"user\" SET private_settings = ? WHERE user_id = ?", nativeQuery = true)
    void setPrivateSettings(@Param("private_settings") String private_settings, @Param("user_id") Long user_id);

}
