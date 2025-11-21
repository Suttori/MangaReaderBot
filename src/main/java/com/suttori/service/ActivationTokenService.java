package com.suttori.service;


import com.suttori.dao.ActivationTokenRepository;
import com.suttori.dao.UserRepository;
import com.suttori.entity.ActivationToken;
import com.suttori.entity.User;
import com.suttori.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.sql.Timestamp;

@Service
public class ActivationTokenService {


    private final Logger logger = LoggerFactory.getLogger(ActivationTokenService.class);

    private ActivationTokenRepository activationTokenRepository;
    private UserRepository userRepository;
    private Util util;


    @Autowired
    public ActivationTokenService(ActivationTokenRepository activationTokenRepository, UserRepository userRepository, Util util) {
        this.activationTokenRepository = activationTokenRepository;
        this.userRepository = userRepository;
        this.util = util;
    }

    public ActivationToken generateToken(Long userId, Timestamp expirationDate) {
        ActivationToken activationToken = new ActivationToken(userId, java.util.UUID.randomUUID().toString(), new Timestamp(System.currentTimeMillis()), expirationDate, "CREATED");
        return activationTokenRepository.save(activationToken);
    }


    public void activatePass(Message message) {
        String[] parts = message.getText().split("_");
        Long activationTokenId = Long.valueOf(parts[1]);
        Long userId = Long.valueOf(parts[2]);
        String token = parts[3];
        ActivationToken activationToken = activationTokenRepository.findByIdAndUserIdAndToken(activationTokenId, userId, token);

        if (activationToken == null) {
            util.sendInfoMessage("Возникла проблема с ссылкой для активации, обратись в поддержку", message.getFrom().getId());
            logger.info("Ссылка для активации бонусов не найдена");
            return;
        }

        if (activationToken.getStatus().equals("USED")) {
            util.sendInfoMessage("Ссылка для активации бонусов уже была использована, если это ошибка, то обратись в поддержку", message.getFrom().getId());
            logger.info("Ссылка для активации бонусов уже была использована");
            return;
        }
        if (activationToken.getExpirationDate().before(new Timestamp(System.currentTimeMillis()))) {
            if (activationToken.getStatus().equals("CREATE")) {
                activationToken.setStatus("EXPIRED");
                activationTokenRepository.save(activationToken);
            }
            util.sendInfoMessage("Срок действия ссылки для активации бонусов истек, обратись в поддержку", message.getFrom().getId());
            logger.info("Срок действия ссылки для активации бонусов истек");
            return;
        }

        User user = userRepository.findByUserId(userId);
        user.setIsPremiumBotUser(true);
        userRepository.save(user);
        activationToken.setStatus("USED");
        activationTokenRepository.save(activationToken);
        util.sendInfoMessage("Бонусы успешно активированы, спасибо за поддержку ♥", message.getFrom().getId());
        logger.info("Бонусы успешно активированы для юзера " + userId);

    }



}
