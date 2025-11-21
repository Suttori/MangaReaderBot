package com.suttori.service;

import com.suttori.telegram.TelegramSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

@Service
public class AwsServerService {

    private final S3Client s3Client;
    private final String bucketName;

    @Autowired
    public AwsServerService(@Value("${aws.accessKeyId}") String accessKey,
                            @Value("${aws.secretKey}") String secretKey,
                            @Value("${aws.region}") String region,
                            @Value("${aws.s3.bucketName}") String bucketName) {
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(() -> AwsBasicCredentials.create(accessKey, secretKey))
                .build();
        this.bucketName = bucketName;
    }

    public URL uploadFileFromUrl(String url, String name, long fileSize) {
        try (InputStream inputStream = new URL(url).openStream()) {
            s3Client.putObject(PutObjectRequest.builder()
                    .bucket(bucketName)
                    .cacheControl("public, max-age=2592000, immutable")
                    .key(name)
                    .build(), RequestBody.fromInputStream(inputStream, fileSize));

            return s3Client.utilities().getUrl(GetUrlRequest.builder()
                    .bucket(bucketName)
                    .key(name)
                    .build());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public URL uploadFileFromUrl(String fileUrl, String name, long fileSize, String referer) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(fileUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Referer", referer);
            try (InputStream inputStream = connection.getInputStream()) {
                s3Client.putObject(PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(name)
                        .cacheControl("public, max-age=2592000, immutable")
                        .build(), RequestBody.fromInputStream(inputStream, fileSize));

                return s3Client.utilities().getUrl(GetUrlRequest.builder()
                        .bucket(bucketName)
                        .key(name)
                        .build());
            }
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при загрузке файла: " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public URL uploadLocalFile(String localFilePath, String name, long fileSize) {
        try (FileInputStream inputStream = new FileInputStream(localFilePath)) {
            s3Client.putObject(PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(name)
                    .cacheControl("public, max-age=2592000, immutable")
                    .build(), RequestBody.fromInputStream(inputStream, fileSize));
            return s3Client.utilities().getUrl(GetUrlRequest.builder()
                    .bucket(bucketName)
                    .key(name)
                    .build());

        } catch (IOException e) {
            throw new RuntimeException("Ошибка при загрузке файла: " + e.getMessage(), e);
        }
    }


    public void deleteFile(String objectKey) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build());
    }
}
