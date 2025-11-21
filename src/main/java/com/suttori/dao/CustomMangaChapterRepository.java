package com.suttori.dao;

import com.suttori.entity.Chapter;

import java.util.List;

public interface CustomMangaChapterRepository {
    void saveAllChapters(List<Chapter> chapters);

}
