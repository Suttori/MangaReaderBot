package com.suttori.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChapterDto {

    private Long id;
    private Integer messageId;
    private Integer backupMessageId;
    private String catalogName;
    private String mangaId;
    private Long mangaDataBaseId;
    private String chapterId;
    private String nextChapterId;
    private String prevChapterId;
    private String type;
    private String format;
    private String name;
    private String telegraphUrl;
    private String vol;
    private String chapter;
    private String status;
    private Timestamp addedAt;
    private String languageCode;
    private Integer pdfMessageId;
    private Integer telegraphMessageId;
    private String pdfStatusDownload;
    private String telegraphStatusDownload;
    private String chapterName;


}
