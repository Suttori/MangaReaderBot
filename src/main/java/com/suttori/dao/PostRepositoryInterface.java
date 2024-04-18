package com.suttori.dao;

import com.suttori.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;

@Repository
public interface PostRepositoryInterface extends JpaRepository<Post, Long> {

    Post findNewPostByPublicationTimeAndChatIdAndChannelId(Timestamp publicationTime, Long chatId,Long channelId);

    @Query(value = "SELECT * from post WHERE publication_time BETWEEN :publication_time1 and :publication_time2", nativeQuery = true)
    ArrayList<Post> findAllScheduled(@Param("publication_time1") Timestamp publication_time1, @Param("publication_time2") Timestamp publication_time2);

    @Query(value = "SELECT * from post WHERE deletion_time BETWEEN :deletion_time1 and :deletion_time2", nativeQuery = true)
    ArrayList<Post> findAllDeletion(@Param("deletion_time1") Timestamp deletion_time1, @Param("deletion_time2") Timestamp deletion_time2);

    @Query(value = "SELECT * from post WHERE chat_id = ? AND channel_id = ? AND publication_time IS NULL AND deletion_time IS NULL AND is_creative IS NULL", nativeQuery = true)
    ArrayList<Post> findNewPostByChatIdaAndChannelId(@Param("chat_id") Long chat_id, @Param("channel_id") Long channelId);

    ArrayList<Post> findNewPostByChannelIdAndPublicationTimeIsNotNull(Long channel_id);

    Post findFirstByIsCreative(boolean isCreative);

    Post findNewPostByChatIdAndMessageIdAndIsCopyPostNull(@Param("chat_id") Long chat_id, @Param("message_id") Integer id);

    Post findFirstByChatIdAndChangeTextAndPublicationTimeIsNullAndDeletionTimeIsNull(@Param("chat_id") Long chat_id, @Param("change_text") boolean change_text);

    Post findFirstByChatIdAndAddButtonAndPublicationTimeIsNullAndDeletionTimeIsNull(@Param("chat_id") Long chat_id, @Param("add_button") boolean add_button);

    Post findFirstByChatIdAndChangePostingTime(Long chat_id, boolean changePostingTime);

    Post findFirstByChatIdOrderByTimeCreateDesc(Long chat_id);

    ArrayList<Post> findAllByChatIdAndIsCreative(Long chat_id, Boolean isCreative);

    @Transactional
    Post findFirstByChatIdAndChangeOneLink(Long chatId, Boolean param);

    Post findFirstByChatIdAndChangeManyLinks(Long chatId, Boolean param);


    @Query(value = "SELECT * from post WHERE chat_id = ? AND message_id = ? AND publication_time IS NULL AND deletion_time IS NULL AND is_creative IS NULL AND is_copy_post IS NOT NULL", nativeQuery = true)
    ArrayList<Post> findCopyPost(@Param("chat_id") Long chat_id, @Param("message_id") Integer message_id);

    @Query(value = "SELECT * from post WHERE chat_id = ? AND message_id = ? AND channel_id = ? AND publication_time IS NULL AND deletion_time IS NULL AND is_creative IS NULL AND is_copy_post IS NOT NULL", nativeQuery = true)
    ArrayList<Post> findCopyPostToDeleteByChannelId(@Param("chat_id") Long chat_id, @Param("message_id") Integer message_id, @Param("channel_id") Long channel_id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE post SET change_one_link = ? WHERE id = ?", nativeQuery = true)
    void setChangeOneLink(@Param("change_one_link") Boolean param, @Param("id") Long id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE post SET change_many_links = ? WHERE id = ?", nativeQuery = true)
    void setChangeManyLinkS(@Param("change_many_links") Boolean param, @Param("id") Long id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE post SET message_id = ? WHERE id = ?", nativeQuery = true)
    void setMessageId(@Param("message_id") Integer message_id, @Param("id") Long id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE post SET change_posting_time = ? WHERE id = ?", nativeQuery = true)
    void setChangePostingTime(@Param("change_posting_time") boolean change_posting_time, @Param("id") Long id);

    @Modifying
    @Query(value = "UPDATE post SET change_text = ? WHERE chat_id = ? AND Message_id = ? AND is_copy_post IS NULL", nativeQuery = true)
    void setChangeText(@Param("change_text") boolean change_text, @Param("chat_id") Long chat_id, @Param("message_id") Integer id);

    @Modifying
    @Query(value = "UPDATE post SET add_button = ? WHERE chat_id = ? AND Message_id = ? AND is_copy_post IS NULL", nativeQuery = true)
    void setAddButton(@Param("add_button") boolean add_button, @Param("chat_id") Long chat_id, @Param("message_id") Integer id);

    @Modifying
    @Query(value = "UPDATE post SET auto_caption = ? WHERE chat_id = ? AND Message_id = ? AND is_copy_post IS NULL", nativeQuery = true)
    void setAutoCaption(@Param("auto_caption") boolean add_button, @Param("chat_id") Long chat_id, @Param("message_id") Integer id);

    @Modifying
    @Query(value = "UPDATE post SET text = ? WHERE chat_id = ? AND Message_id = ? AND is_copy_post IS NULL", nativeQuery = true)
    void setText(@Param("text") String text, @Param("chat_id") Long chat_id, @Param("message_id") Integer id);

    @Modifying
    @Query(value = "UPDATE post SET caption = ? WHERE chat_id = ? AND message_id = ? AND is_copy_post IS NULL", nativeQuery = true)
    void setCaption(@Param("caption") String caption, @Param("chat_id") Long chat_id, @Param("message_id") Integer id);

    @Modifying
    @Query(value = "UPDATE post SET button = ? WHERE chat_id = ? AND Message_id = ? AND is_copy_post IS NULL", nativeQuery = true)
    void setButton(@Param("button") String button, @Param("chat_id") Long chat_id, @Param("message_id") Integer id);

    @Modifying
    @Query(value = "UPDATE post SET add_button = ? WHERE chat_id = ? AND add_button = ?", nativeQuery = true)
    void setAddButtonByParameter(@Param("add_button") boolean add_button, @Param("chat_id") Long chat_id, @Param("add_button") boolean new_add_button);

    @Modifying
    @Query(value = "UPDATE post SET disable_notification = ? WHERE chat_id = ? AND Message_id = ? AND is_copy_post IS NULL", nativeQuery = true)
    void setDisableNotification(@Param("disable_notification") Boolean disable_notification, @Param("chat_id") Long chat_id, @Param("message_id") Integer id);

    @Modifying
    @Query(value = "UPDATE post SET disable_web_page_preview = ? WHERE chat_id = ? AND Message_id = ? AND is_copy_post IS NULL", nativeQuery = true)
    void setDisableWebPreview(@Param("disable_web_page_preview") Boolean disable_web_page_preview, @Param("chat_id") Long chat_id, @Param("message_id") Integer id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE post SET is_forward_message = ? WHERE chat_id = ? AND Message_id = ? AND is_copy_post IS NULL", nativeQuery = true)
    void setForwardMessage(@Param("is_forward_message") Boolean is_forward_message, @Param("chat_id") Long chat_id, @Param("message_id") Integer id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE post SET publication_time = ? WHERE chat_id = ? AND publication_time IS NULL AND is_creative IS NULL", nativeQuery = true)
    void setPublicationTime(@Param("publication_time") LocalDateTime publication_time, @Param("chat_id") Long chat_id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE post SET publication_time = ? WHERE id = ?", nativeQuery = true)
    void setPublicationTimeById(@Param("publication_time") LocalDateTime publication_time, @Param("chat_id") Long id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE post SET publication_time = ? WHERE id = ?", nativeQuery = true)
    void updatePublicationTime(@Param("publication_time") Timestamp publication_time, @Param("id") Long id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE post SET channel_id = ? WHERE chat_id = ? AND publication_time IS NULL AND is_creative IS NULL", nativeQuery = true)
    void setChannelId(@Param("channel_id") Long publication_time, @Param("chat_id") Long chat_id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE post SET milliseconds_for_deleting = ? WHERE id = ?", nativeQuery = true)
    void setMillisecondsForDeleting(@Param("milliseconds_for_deleting") Long millisecondsForDeleting, @Param("id") Long postId);

    @Transactional
    @Modifying
    @Query(value = "UPDATE post SET deletion_time = ? WHERE id = ?", nativeQuery = true)
    void setDeletingTime(@Param("deletion_time") Timestamp deletionTime, @Param("id") Long postId);

    @Transactional
    @Modifying
    @Query(value = "UPDATE post SET milliseconds_for_deleting = NULL WHERE id = ?", nativeQuery = true)
    void setNullMillisecondsForDeleting(@Param("id") Long postId);

    @Modifying
    @Query(value = "UPDATE post SET button = ? WHERE chat_id = ? AND add_button = ?", nativeQuery = true)
    void deleteButton(@Param("button") String button, @Param("chat_id") Long chat_id, @Param("add_button") boolean new_add_button);

    @Transactional
    @Modifying
    @Query(value = "DELETE from post WHERE chat_id = ? AND channel_id = ? AND publication_time IS NULL AND is_creative IS NULL", nativeQuery = true)
    Integer deleteAllByChatId(@Param("chat_id") Long chat_id, @Param("channel_id") Long channel_id);

    @Transactional
    @Modifying
    @Query(value = "DELETE from post WHERE channel_id = ?", nativeQuery = true)
    Integer deleteAllByChannelId(@Param("channel_id") Long channel_id);

    @Transactional
    @Modifying
    @Query(value = "DELETE from post WHERE chat_id = ? AND message_id = ? AND publication_time IS NULL AND deletion_time IS NULL AND is_creative IS NULL AND is_copy_post IS NOT NULL", nativeQuery = true)
    Integer deleteAllCopyPost(@Param("chat_id") Long chat_id, @Param("message_id") Integer message_id);

}
