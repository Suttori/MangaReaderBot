package com.suttori.entity.MangaDesu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MangaDataDesu {

    private Long id;
    private String name;
    private String russian;
    private String kind;
    private MangaImage image;
    private String url;
    private String reading;
    private Integer ongoing;
    private Integer anons;
    private Integer adult;
    private String status;
    private Long aired_on;
    private Long released_on;
    private Double score;
    private Integer score_users;
    private Integer views;
    private String description;
    private Long checked;
    private Long updated;
    private List<MangaGenre> genres;
    private List<MangaTranslator> translators;
    private String synonyms;
    private Long thread_id;
    private Long shikimori_id;
    private Long myanimelist_id;
    private String mangadex_id;
    private MangaChapters chapters;
    private String age_limit;
    private String trans_status;
    private MangaPages pages;

    public MangaPages getPages() {
        return pages;
    }

    public void setPages(MangaPages pages) {
        this.pages = pages;
    }

    public String getMangadex_id() {
        return mangadex_id;
    }

    public void setMangadex_id(String mangadex_id) {
        this.mangadex_id = mangadex_id;
    }

    public List<MangaGenre> getGenres() {
        return genres;
    }

    public void setGenres(List<MangaGenre> genres) {
        this.genres = genres;
    }

    public String getTrans_status() {
        return trans_status;
    }

    public void setTrans_status(String trans_status) {
        this.trans_status = trans_status;
    }

    public String getAge_limit() {
        return age_limit;
    }

    public void setAge_limit(String age_limit) {
        this.age_limit = age_limit;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRussian() {
        return russian;
    }

    public void setRussian(String russian) {
        this.russian = russian;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public MangaImage getImage() {
        return image;
    }

    public void setImage(MangaImage image) {
        this.image = image;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getReading() {
        return reading;
    }

    public void setReading(String reading) {
        this.reading = reading;
    }

    public Integer getOngoing() {
        return ongoing;
    }

    public void setOngoing(Integer ongoing) {
        this.ongoing = ongoing;
    }

    public Integer getAnons() {
        return anons;
    }

    public void setAnons(Integer anons) {
        this.anons = anons;
    }

    public Integer getAdult() {
        return adult;
    }

    public void setAdult(Integer adult) {
        this.adult = adult;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getAired_on() {
        return aired_on;
    }

    public void setAired_on(Long aired_on) {
        this.aired_on = aired_on;
    }

    public Long getReleased_on() {
        return released_on;
    }

    public void setReleased_on(Long released_on) {
        this.released_on = released_on;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Integer getScore_users() {
        return score_users;
    }

    public void setScore_users(Integer score_users) {
        this.score_users = score_users;
    }

    public Integer getViews() {
        return views;
    }

    public void setViews(Integer views) {
        this.views = views;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getChecked() {
        return checked;
    }

    public void setChecked(Long checked) {
        this.checked = checked;
    }

    public Long getUpdated() {
        return updated;
    }

    public void setUpdated(Long updated) {
        this.updated = updated;
    }

    public List<MangaTranslator> getTranslators() {
        return translators;
    }

    public void setTranslators(List<MangaTranslator> translators) {
        this.translators = translators;
    }

    public String getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(String synonyms) {
        this.synonyms = synonyms;
    }

    public Long getThread_id() {
        return thread_id;
    }

    public void setThread_id(Long thread_id) {
        this.thread_id = thread_id;
    }

    public Long getShikimori_id() {
        return shikimori_id;
    }

    public void setShikimori_id(Long shikimori_id) {
        this.shikimori_id = shikimori_id;
    }

    public Long getMyanimelist_id() {
        return myanimelist_id;
    }

    public void setMyanimelist_id(Long myanimelist_id) {
        this.myanimelist_id = myanimelist_id;
    }

    public MangaChapters getChapters() {
        return chapters;
    }

    public void setChapters(MangaChapters chapters) {
        this.chapters = chapters;
    }

}


