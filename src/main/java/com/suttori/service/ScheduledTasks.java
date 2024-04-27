package com.suttori.service;


import com.suttori.dao.LastActivityRepository;
import com.suttori.dao.NotificationChapterMappingRepository;
import com.suttori.dao.NotificationEntityRepository;
import com.suttori.dao.UserRepository;
import com.suttori.entity.LastActivity;
import com.suttori.entity.MangaDesu.NotificationEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Component
public class ScheduledTasks {

    private final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);

    private SenderService senderService;
    private MangaService mangaService;
    private AdminService adminService;
    private NotificationEntityRepository notificationEntityRepository;
    private NotificationChapterMappingRepository notificationChapterMappingRepository;
    private LastActivityRepository lastActivityRepository;
    private UserRepository userRepository;

    @Autowired
    public ScheduledTasks(SenderService senderService, MangaService mangaService, AdminService adminService, NotificationEntityRepository notificationEntityRepository, NotificationChapterMappingRepository notificationChapterMappingRepository, LastActivityRepository lastActivityRepository, UserRepository userRepository) {
        this.senderService = senderService;
        this.mangaService = mangaService;
        this.adminService = adminService;
        this.notificationEntityRepository = notificationEntityRepository;
        this.notificationChapterMappingRepository = notificationChapterMappingRepository;
        this.lastActivityRepository = lastActivityRepository;
        this.userRepository = userRepository;
    }

    //@Scheduled(cron = "0 */40 * * * *")
    public void ScheduledNotification() {
//        logger.info("ScheduledNotification");
//        Map<String, List<Long>> prepareList = notificationEntityRepository.findAll().stream()
//                .collect(Collectors.groupingBy(NotificationEntity::getMangaId,
//                        Collectors.mapping(NotificationEntity::getUserId, Collectors.toList())));
//        for (String key : prepareList.keySet()) {
//            Long lastChapter = Long.valueOf(mangaService.getMangaData(key).getChapters().getLast().getCh());
//            if (!notificationChapterMappingRepository.findByMangaId(key).getChapter().equals(lastChapter)) {
//                notificationChapterMappingRepository.setChapter(lastChapter, key);
//                senderService.sendNotificationToUsers(prepareList.get(key), key, lastChapter);
//            }
//        }
    }

    //@Scheduled(cron = "0 57 22 * * *")
//    @Scheduled(cron = "0 * * * * *")
    public void writeLastActivity() {
        logger.info("writeLastActivity");
        LastActivity lastActivity = new LastActivity();
        lastActivity.setDate(new Timestamp(System.currentTimeMillis()));
        lastActivity.setActiveUsers(userRepository.findAllByLastActivityAfter(new Timestamp(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1))).size());
        lastActivityRepository.save(lastActivity);
    }

    //@Scheduled(cron = "0 */30 * * * *")
    public void writeDownloadChaptersStat() {
        logger.info("writeDownloadChaptersStat");
        adminService.writeDownloadChaptersStat();
    }

    //@Scheduled(cron = "0 0 01 * * *")
//    @Scheduled(cron = "*/40 * * * * *")
    public void doBackup() {
        logger.info("doBackup");
        mangaService.doBackup();
    }
}
