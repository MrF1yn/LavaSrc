package com.github.topi314.lavasrc.spotify.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Objects;

@JsonIgnoreProperties(
        ignoreUnknown = true
)
public class SpotifyArtistInfo {
    private final String name;
    private final String id;
    private final int popularity;
    private final List<String> genres;

    public SpotifyArtistInfo() {
        this.name = null;
        this.id = null;
        this.popularity = 0;
        this.genres = null;
    }

    public SpotifyArtistInfo(String name, String id, int popularity, List<String> genres) {
        this.name = name;
        this.id = id;
        this.popularity = popularity;
        this.genres = genres;
    }

    public String getName() {
        return this.name;
    }

    public String getId() {
        return this.id;
    }

    public int getPopularity() {
        return this.popularity;
    }

    public List<String> getGenres() {
        return this.genres;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            SpotifyArtistInfo that = (SpotifyArtistInfo)o;
            return this.name.equals(that.name) && this.id.equals(that.id);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.name, this.id});
    }

    public String toString() {
        return "SpotifyArtistInfo{name='" + this.name + "', id='" + this.id + "', popularity=" + this.popularity + ", genres=" + this.genres + "}";
    }
}

