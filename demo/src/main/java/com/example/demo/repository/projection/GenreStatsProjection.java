package com.example.demo.repository.projection;

public interface GenreStatsProjection {
    String getLanguage();

    String getGenre();

    Long getTotalCount();

    Long getCorrectCount();
}
