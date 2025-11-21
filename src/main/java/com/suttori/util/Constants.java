package com.suttori.util;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class Constants {

    private static final Map<String, String> GENRE_PARAM_DESU_ME;
    private static final Map<String, String> SORT_PARAM_DESU_ME;
    private static final Map<String, String> STATUS_PARAM_DESU_ME;
    private static final Map<String, String> TYPE_PARAM_DESU_ME;

    private static final Map<String, String> SORT_PARAM_MANGADEX;
    private static final Map<String, String> STATUS_PARAM_MANGADEX;
    private static final Map<String, String> GENRE_PARAM_MANGADEX;
    private static final Map<String, String> CONTENT_RATING_PARAM_MANGADEX;
    private static final Map<String, String> MAGAZINE_DEMOGRAPHIC_PARAM_MANGADEX;
    private static final Map<String, String> THEME_PARAM_MANGADEX;
    private static final Map<String, String> FORMAT_PARAM_MANGADEX;

    private static final Map<String, String> REV_SORT_PARAM_DESU_ME;
    private static final Map<String, String> REV_TYPE_PARAM_DESU_ME;

    private static final Map<String, String> REV_SORT_PARAM_MANGADEX;
    private static final Map<String, String> REV_TYPE_PARAM_MANGADEX;

    static {
        Map<String, String> genreParamDesuMe = new LinkedHashMap<>();
        genreParamDesuMe.put("Безумие", "Dementia");
        genreParamDesuMe.put("Боевые искусства", "Martial_Arts");
        genreParamDesuMe.put("В цвете", "Color");
        genreParamDesuMe.put("Вампиры", "Vampire");
        genreParamDesuMe.put("Веб", "Web");
        genreParamDesuMe.put("Гарем", "Harem");
        genreParamDesuMe.put("Героическое фэнтези", "Heroic_Fantasy");
        genreParamDesuMe.put("Демоны", "Demons");
        genreParamDesuMe.put("Детектив", "Mystery");
        genreParamDesuMe.put("Дзёсей", "Josei");
        genreParamDesuMe.put("Драма", "Drama");
        genreParamDesuMe.put("Ёнкома", "Yonkoma");
        genreParamDesuMe.put("Игры", "Game");
        genreParamDesuMe.put("Исекай", "Isekai");
        genreParamDesuMe.put("Исторический", "Historical");
        //genreParamDesuMe.put("Комедия", "Comedy");
        genreParamDesuMe.put("Космос", "Space");
        genreParamDesuMe.put("ЛитRPG", "LitRPG");
        genreParamDesuMe.put("Магия", "Magic");
        genreParamDesuMe.put("Меха", "Mecha");
        genreParamDesuMe.put("Мистика", "Mystic");
        genreParamDesuMe.put("Музыка", "Music");
        genreParamDesuMe.put("Научная фантастика", "Sci-Fi");
        genreParamDesuMe.put("Пародия", "Parody");
        genreParamDesuMe.put("Повседневность", "Slice_of_Life");
        genreParamDesuMe.put("Постапокалиптика", "Post_Apocalyptic");
        genreParamDesuMe.put("Приключения", "Adventure");
        genreParamDesuMe.put("Психологическое", "Psychological");
        genreParamDesuMe.put("Романтика", "Romance");
        //genreParamDesuMe.put("Самураи", "Samurai");
        genreParamDesuMe.put("Сверхъестественное", "Supernatural");
        genreParamDesuMe.put("Сёдзе", "Shoujo");
        genreParamDesuMe.put("Сёдзе ай", "Shoujo_Ai"); //удалено
        genreParamDesuMe.put("Сейнен", "Seinen");
        genreParamDesuMe.put("Сёнен", "Shounen");
        genreParamDesuMe.put("Сёнен ай", "Shounen_Ai"); //удалено
        genreParamDesuMe.put("Смена пола", "Gender_Bender"); //удалено
        genreParamDesuMe.put("Спорт", "Sports");
        genreParamDesuMe.put("Супер сила", "Super_Power");
        genreParamDesuMe.put("Трагедия", "Tragedy");
        genreParamDesuMe.put("Триллер", "Thriller");
        genreParamDesuMe.put("Ужасы", "Horror");
        genreParamDesuMe.put("Фантастика", "Fiction");
        genreParamDesuMe.put("Фэнтези", "Fantasy");
        genreParamDesuMe.put("Хентай", "Hentai");
        genreParamDesuMe.put("Школа", "School");
        genreParamDesuMe.put("Экшен", "Action");
        genreParamDesuMe.put("Этти", "Ecchi");
        genreParamDesuMe.put("Юри", "Yuri"); //удалено
        genreParamDesuMe.put("Яой", "Yaoi"); //удалено
        GENRE_PARAM_DESU_ME = Collections.unmodifiableMap(genreParamDesuMe);
    }

    static {
        Map<String, String> sortParamDesuMe = new LinkedHashMap<>();
        sortParamDesuMe.put("По популярности", "popular");
        sortParamDesuMe.put("По названию", "name");
        sortParamDesuMe.put("По обновлению", "updated");
        //sortParamDesuMe.put("По добавлению", "id");
        SORT_PARAM_DESU_ME = Collections.unmodifiableMap(sortParamDesuMe);
    }

    static {
        Map<String, String> revSortParamDesuMe = new LinkedHashMap<>();
        revSortParamDesuMe.put("popular", "По популярности");
        revSortParamDesuMe.put("name", "По названию");
        revSortParamDesuMe.put("updated", "По обновлению");
        REV_SORT_PARAM_DESU_ME = Collections.unmodifiableMap(revSortParamDesuMe);
    }

    static {
        Map<String, String> statusParamDesuMe = new LinkedHashMap<>();
        statusParamDesuMe.put("Выходит", "ongoing");
        statusParamDesuMe.put("Издано", "released");
        statusParamDesuMe.put("Переводится", "continued");
        statusParamDesuMe.put("Завершено", "completed");
        STATUS_PARAM_DESU_ME = Collections.unmodifiableMap(statusParamDesuMe);
    }

    static {
        Map<String, String> typeParamDesuMe = new LinkedHashMap<>();
        typeParamDesuMe.put("Манга", "manga");
        typeParamDesuMe.put("Манхва", "manhwa");
        typeParamDesuMe.put("Манхьхуа", "manhua");
        typeParamDesuMe.put("Ваншот", "one_shot");
        typeParamDesuMe.put("Комикс", "comics");
        TYPE_PARAM_DESU_ME = Collections.unmodifiableMap(typeParamDesuMe);
    }

    static {
        Map<String, String> sortParamMangaDex = new LinkedHashMap<>();
        sortParamMangaDex.put("С наивысшим рейтингом", "order[rating]\ndesc");
        sortParamMangaDex.put("С наименьшим рейтингом", "order[rating]\nasc");
        sortParamMangaDex.put("Недавно изданное", "order[year]\ndesc");
        sortParamMangaDex.put("Издано давно", "order[year]\nasc");
        sortParamMangaDex.put("По алфавиту", "order[title]\ndesc");
        sortParamMangaDex.put("По алфавиту с конца", "order[title]\nasc");
        sortParamMangaDex.put("Последние обновления", "order[latestUploadedChapter]\ndesc");
        sortParamMangaDex.put("Давние обновления", "order[latestUploadedChapter]\nasc");
        sortParamMangaDex.put("Недавно добавлено", "order[createdAt]\ndesc");
        sortParamMangaDex.put("Добавлено давно", "order[createdAt]\nasc");
        SORT_PARAM_MANGADEX = Collections.unmodifiableMap(sortParamMangaDex);
    }

    static {
        Map<String, String> revSortParamMangaDex = new LinkedHashMap<>();
        revSortParamMangaDex.put("order[rating]\ndesc", "С наивысшим рейтингом");
        revSortParamMangaDex.put("order[rating]\nasc", "С наименьшим рейтингом");
        revSortParamMangaDex.put("order[year]\ndesc", "Недавно изданное");
        revSortParamMangaDex.put("order[year]\nasc", "Издано давно");
        revSortParamMangaDex.put("order[title]\ndesc", "По алфавиту");
        revSortParamMangaDex.put("order[title]\nasc", "По алфавиту с конца");
        revSortParamMangaDex.put("order[latestUploadedChapter]\ndesc", "Последние обновления");
        revSortParamMangaDex.put("order[latestUploadedChapter]\nasc", "Давние обновления");
        revSortParamMangaDex.put("order[createdAt]\ndesc", "Недавно добавлено");
        revSortParamMangaDex.put("order[createdAt]\nasc", "Добавлено давно");
        REV_SORT_PARAM_MANGADEX = Collections.unmodifiableMap(revSortParamMangaDex);
    }


    static {
        Map<String, String> statusParamMangaDex = new LinkedHashMap<>();
        statusParamMangaDex.put("Выходит", "ongoing");
        statusParamMangaDex.put("Пауза", "hiatus");
        statusParamMangaDex.put("Не завершена", "cancelled");
        statusParamMangaDex.put("Завершено", "completed");
        STATUS_PARAM_MANGADEX = Collections.unmodifiableMap(statusParamMangaDex);
    }

    static {
        Map<String, String> genreParamMangaDex = new LinkedHashMap<>();
        genreParamMangaDex.put("Триллер", "Thriller");
        genreParamMangaDex.put("Научная фантастика", "Sci-Fi");
        genreParamMangaDex.put("Исторический", "Historical");
        genreParamMangaDex.put("Экшн", "Action");
        genreParamMangaDex.put("Психологический", "Psychological");
        genreParamMangaDex.put("Комедия", "Comedy");
        genreParamMangaDex.put("Меха", "Mecha");
        genreParamMangaDex.put("Boys' Love", "Boys' Love");
        genreParamMangaDex.put("Криминал", "Crime");
        genreParamMangaDex.put("Спорт", "Sports");
        genreParamMangaDex.put("Супергерой", "Superhero");
        genreParamMangaDex.put("Девушки и магия", "Magical Girls");
        genreParamMangaDex.put("Приключение", "Adventure");
        genreParamMangaDex.put("Girls' Love", "Girls' Love");
        genreParamMangaDex.put("Wuxia", "Wuxia");
        genreParamMangaDex.put("Isekai", "Isekai");
        genreParamMangaDex.put("Философский", "Philosophical");
        genreParamMangaDex.put("Драма", "Drama");
        genreParamMangaDex.put("Медицинский", "Medical");
        genreParamMangaDex.put("Ужасы", "Horror");
        genreParamMangaDex.put("Фэнтези", "Fantasy");
        genreParamMangaDex.put("Повседневность", "Slice of Life");
        genreParamMangaDex.put("Мистика", "Mystery");
        genreParamMangaDex.put("Трагедия", "Tragedy");
        GENRE_PARAM_MANGADEX = Collections.unmodifiableMap(genreParamMangaDex);
    }

    static {
        Map<String, String> ratingContentParamMangaDex = new LinkedHashMap<>();
        ratingContentParamMangaDex.put("Безопастный контент", "safe");
        ratingContentParamMangaDex.put("Вызывающий контент", "suggestive");
        ratingContentParamMangaDex.put("Эротика", "erotica");
        ratingContentParamMangaDex.put("18+", "pornographic");
        CONTENT_RATING_PARAM_MANGADEX = Collections.unmodifiableMap(ratingContentParamMangaDex);
    }

    static {
        Map<String, String> magazineDemographicParamMangaDex = new LinkedHashMap<>();
        magazineDemographicParamMangaDex.put("Shounen", "shounen");
        magazineDemographicParamMangaDex.put("Shoujo", "shoujo");
        magazineDemographicParamMangaDex.put("Josei", "josei");
        magazineDemographicParamMangaDex.put("Seinen", "seinen");
        MAGAZINE_DEMOGRAPHIC_PARAM_MANGADEX = Collections.unmodifiableMap(magazineDemographicParamMangaDex);
    }

    static {
        Map<String, String> themeParamMangaDex = new LinkedHashMap<>();
        themeParamMangaDex.put("Реинкарнация", "Reincarnation");
        themeParamMangaDex.put("Путешествие во времени", "Time Travel");
        //themeParamMangaDex.put("Genderswap", "Genderswap");
        //themeParamMangaDex.put("Loli", "Loli");
        themeParamMangaDex.put("Традиционные игры", "Traditional Games");
        themeParamMangaDex.put("Монстры", "Monsters");
        themeParamMangaDex.put("Демоны", "Demons");
        themeParamMangaDex.put("Призраки", "Ghosts");
        themeParamMangaDex.put("Животные", "Animals");
        themeParamMangaDex.put("Ниндзя", "Ninja");
        //themeParamMangaDex.put("Incest", "Incest");
        themeParamMangaDex.put("Выживание", "Survival");
        themeParamMangaDex.put("Зомби", "Zombies");
        themeParamMangaDex.put("Обратный гарем", "Reverse Harem");
        themeParamMangaDex.put("Боевые искусства", "Martial Arts");
        themeParamMangaDex.put("Самураи", "Samurai");
        themeParamMangaDex.put("Мафия", "Mafia");
        themeParamMangaDex.put("Виртуальная реальность", "Virtual Reality");
        themeParamMangaDex.put("Офисные работники", "Office Workers");
        themeParamMangaDex.put("Видеоигры", "Video Games");
        themeParamMangaDex.put("Постапокалипсис", "Post-Apocalyptic");
        themeParamMangaDex.put("Crossdressing", "Crossdressing");
        themeParamMangaDex.put("Магия", "Magic");
        themeParamMangaDex.put("Гарем", "Harem");
        themeParamMangaDex.put("Военные", "Military");
        themeParamMangaDex.put("Школьная жизнь", "School Life");
        themeParamMangaDex.put("Злодейка", "Villainess");
        themeParamMangaDex.put("Вампиры", "Vampires");
        themeParamMangaDex.put("Хулиганы", "Delinquents");
        themeParamMangaDex.put("Монстродевушки", "Monster Girls");
        //themeParamMangaDex.put("Shota", "Shota");
        themeParamMangaDex.put("Полиция", "Police");
        themeParamMangaDex.put("Инопланетяне", "Aliens");
        themeParamMangaDex.put("Кулинария", "Cooking");
        themeParamMangaDex.put("Музыка", "Music");
        themeParamMangaDex.put("Гяру", "Gyaru");
        THEME_PARAM_MANGADEX = Collections.unmodifiableMap(themeParamMangaDex);
    }

    static {
        Map<String, String> formatParamMangaDex = new LinkedHashMap<>();
        formatParamMangaDex.put("Ваншот", "Oneshot");
        formatParamMangaDex.put("Победитель премии", "Award Winning");
        formatParamMangaDex.put("Официально раскрашено", "Official Colored");
        formatParamMangaDex.put("Вертикальное чтение", "Long Strip");
        formatParamMangaDex.put("Раскрашено фанатами", "Fan Colored");
        formatParamMangaDex.put("Самоизданное", "Self-Published");
        formatParamMangaDex.put("4-Koma", "4-Koma");
        formatParamMangaDex.put("Додзинси", "Doujinshi");
        formatParamMangaDex.put("Веб-комикс", "Web Comic");
        formatParamMangaDex.put("Адаптация", "Adaptation");
        formatParamMangaDex.put("Полностью в цвете", "Full Color");
        FORMAT_PARAM_MANGADEX = Collections.unmodifiableMap(formatParamMangaDex);
    }

    static {
        Map<String, String> revTypeParamDesuMe = new LinkedHashMap<>();
        revTypeParamDesuMe.put("kinds", "Тип");
        revTypeParamDesuMe.put("genres", "Жанр");
        REV_TYPE_PARAM_DESU_ME = Collections.unmodifiableMap(revTypeParamDesuMe);
    }

    static {
        Map<String, String> revTypeParamMangaDex = new LinkedHashMap<>();
        revTypeParamMangaDex.put("format", "Формат");
        revTypeParamMangaDex.put("contentRating", "Возрастной рейтинг");
        revTypeParamMangaDex.put("magazineDemographic", "Категория манги");
        revTypeParamMangaDex.put("status", "Статус");
        revTypeParamMangaDex.put("genre", "Жанр");
        revTypeParamMangaDex.put("theme", "Тематика");
        REV_TYPE_PARAM_MANGADEX = Collections.unmodifiableMap(revTypeParamMangaDex);
    }




    public static String getGenreParamDesuMe(String key) {
        return GENRE_PARAM_DESU_ME.get(key);
    }

    public static Map<String, String> getAllGenreParamDesuMe() {
        return GENRE_PARAM_DESU_ME;
    }

    public static String getSortParamDesuMe(String key) {
        return SORT_PARAM_DESU_ME.get(key);
    }

    public static String getRevSortParamDesuMe(String key) {
        return REV_SORT_PARAM_DESU_ME.get(key);
    }

    public static String getRevTypeParamDesuMe(String key) {
        return REV_TYPE_PARAM_DESU_ME.get(key);
    }

    public static String getRevTypeParamMangaDex(String key) {
        return REV_TYPE_PARAM_MANGADEX.get(key);
    }

    public static Map<String, String> getAllSortParamDesuMe() {
        return SORT_PARAM_DESU_ME;
    }

    public static String getStatusParamDesuMe(String key) {
        return STATUS_PARAM_DESU_ME.get(key);
    }

    public static Map<String, String> getAllStatusParamDesuMe() {
        return STATUS_PARAM_DESU_ME;
    }

    public static String getTypeParamDesuMe(String key) {
        return TYPE_PARAM_DESU_ME.get(key);
    }

    public static Map<String, String> getAllTypeParamDesuMe() {
        return TYPE_PARAM_DESU_ME;
    }

    public static String getSortParamMangaDex(String key) {
        return SORT_PARAM_MANGADEX.get(key);
    }

    public static Map<String, String> getAllSortParamMangaDex() {
        return SORT_PARAM_MANGADEX;
    }

    public static String getStatusParamMangaDex(String key) {
        return STATUS_PARAM_MANGADEX.get(key);
    }

    public static Map<String, String> getAllStatusParamMangaDex() {
        return STATUS_PARAM_MANGADEX;
    }

    public static String getGenreParamMangaDex(String key) {
        return GENRE_PARAM_MANGADEX.get(key);
    }

    public static Map<String, String> getAllGenreParamMangaDex() {
        return GENRE_PARAM_MANGADEX;
    }

    public static String getContentRatingParamMangaDex(String key) {
        return CONTENT_RATING_PARAM_MANGADEX.get(key);
    }

    public static Map<String, String> getAllContentRatingParamMangaDex() {
        return CONTENT_RATING_PARAM_MANGADEX;
    }

    public static String getMagazineDemographicParamMangaDex(String key) {
        return MAGAZINE_DEMOGRAPHIC_PARAM_MANGADEX.get(key);
    }

    public static Map<String, String> getAllMagazineDemographicParamMangaDex() {
        return MAGAZINE_DEMOGRAPHIC_PARAM_MANGADEX;
    }

    public static String getThemeParamMangaDex(String key) {
        return THEME_PARAM_MANGADEX.get(key);
    }

    public static Map<String, String> getAllThemeParamMangaDex() {
        return THEME_PARAM_MANGADEX;
    }

    public static String getFormatParamMangaDex(String key) {
        return FORMAT_PARAM_MANGADEX.get(key);
    }

    public static Map<String, String> getAllFormatParamMangaDex() {
        return FORMAT_PARAM_MANGADEX;
    }

    public static String getRevSortParamMangaDex(String key) {
        return REV_SORT_PARAM_MANGADEX.get(key);
    }

}
