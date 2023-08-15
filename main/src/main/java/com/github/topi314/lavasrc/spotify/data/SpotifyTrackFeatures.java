package com.github.topi314.lavasrc.spotify.data;//



import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(
        ignoreUnknown = true
)
public class SpotifyTrackFeatures {
    private final double acousticness;
    private final String analysis_url;
    private final double danceability;
    private final long duration_ms;
    private final double energy;
    private final String id;
    private final double instrumentalness;
    private final int key;
    private final double liveness;
    private final double loudness;
    private final int mode;
    private final double speechiness;
    private final double tempo;
    private final int time_signature;
    private final String track_href;
    private final String type;
    private final String uri;
    private final double valence;

    public SpotifyTrackFeatures() {
        this.acousticness = 0.0;
        this.analysis_url = null;
        this.danceability = 0.0;
        this.duration_ms = 0L;
        this.energy = 0.0;
        this.id = null;
        this.instrumentalness = 0.0;
        this.key = 0;
        this.liveness = 0.0;
        this.loudness = 0.0;
        this.mode = 0;
        this.speechiness = 0.0;
        this.tempo = 0.0;
        this.time_signature = 0;
        this.track_href = null;
        this.type = null;
        this.uri = null;
        this.valence = 0.0;
    }

    public SpotifyTrackFeatures(double acousticness, String analysis_url, double danceability, long duration_ms, double energy, String id, double instrumentalness, int key, double liveness, double loudness, int mode, double speechiness, double tempo, int time_signature, String track_href, String type, String uri, double valence) {
        this.acousticness = acousticness;
        this.analysis_url = analysis_url;
        this.danceability = danceability;
        this.duration_ms = duration_ms;
        this.energy = energy;
        this.id = id;
        this.instrumentalness = instrumentalness;
        this.key = key;
        this.liveness = liveness;
        this.loudness = loudness;
        this.mode = mode;
        this.speechiness = speechiness;
        this.tempo = tempo;
        this.time_signature = time_signature;
        this.track_href = track_href;
        this.type = type;
        this.uri = uri;
        this.valence = valence;
    }

    public double getAcousticness() {
        return this.acousticness;
    }

    public String getAnalysis_url() {
        return this.analysis_url;
    }

    public double getDanceability() {
        return this.danceability;
    }

    public long getDuration_ms() {
        return this.duration_ms;
    }

    public double getEnergy() {
        return this.energy;
    }

    public String getId() {
        return this.id;
    }

    public double getInstrumentalness() {
        return this.instrumentalness;
    }

    public int getKey() {
        return this.key;
    }

    public double getLiveness() {
        return this.liveness;
    }

    public double getLoudness() {
        return this.loudness;
    }

    public int getMode() {
        return this.mode;
    }

    public double getSpeechiness() {
        return this.speechiness;
    }

    public double getTempo() {
        return this.tempo;
    }

    public int getTime_signature() {
        return this.time_signature;
    }

    public String getTrack_href() {
        return this.track_href;
    }

    public String getType() {
        return this.type;
    }

    public String getUri() {
        return this.uri;
    }

    public double getValence() {
        return this.valence;
    }

    public String toString() {
        return "SpotifyTrackFeatures{acousticness=" + this.acousticness + ", analysis_url='" + this.analysis_url + "', danceability=" + this.danceability + ", duration_ms=" + this.duration_ms + ", energy=" + this.energy + ", id='" + this.id + "', instrumentalness=" + this.instrumentalness + ", key=" + this.key + ", liveness=" + this.liveness + ", loudness=" + this.loudness + ", mode=" + this.mode + ", speechiness=" + this.speechiness + ", tempo=" + this.tempo + ", time_signature=" + this.time_signature + ", track_href='" + this.track_href + "', type='" + this.type + "', uri='" + this.uri + "', valence=" + this.valence + "}";
    }
}
