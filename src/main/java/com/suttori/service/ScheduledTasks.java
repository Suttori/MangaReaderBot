package com.suttori.service;


import com.suttori.dao.LastActivityRepository;
import com.suttori.dao.UserRepository;
import com.suttori.entity.LastActivity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;


@Component
public class ScheduledTasks {

    private final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);

    private MangaService mangaService;
    private AdminService adminService;
    private LastActivityRepository lastActivityRepository;
    private UserRepository userRepository;
    private DesuMeService desuMeService;
    private MangaDexService mangaDexService;

    @Autowired
    public ScheduledTasks(MangaService mangaService, AdminService adminService, LastActivityRepository lastActivityRepository, UserRepository userRepository, DesuMeService desuMeService, MangaDexService mangaDexService) {
        this.mangaService = mangaService;
        this.adminService = adminService;
        this.lastActivityRepository = lastActivityRepository;
        this.userRepository = userRepository;
        this.desuMeService = desuMeService;
        this.mangaDexService = mangaDexService;
    }

    @Scheduled(cron = "0 */59 * * * *")
    public void ScheduledNotification() {
        logger.info("ScheduledNotificationDesuMe");
        desuMeService.sendNotificationAboutNewChapter();
        logger.info("ScheduledNotificationMangaDex");
        mangaDexService.sendNotificationAboutNewChapter();
    }

    @Scheduled(cron = "0 57 22 * * *")
    public void writeLastActivity() {
        logger.info("writeLastActivity");
        LastActivity lastActivity = new LastActivity();
        lastActivity.setDate(new Timestamp(System.currentTimeMillis()));
        lastActivity.setActiveUsers(userRepository.findAllByLastActivityAfter(new Timestamp(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1))).size());
        lastActivityRepository.save(lastActivity);
    }

    @Scheduled(cron = "0 00 12 * * *")
//    @Scheduled(cron = "0 */60 * * * *")
//    @Scheduled(cron = "0 * * * * *")
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
