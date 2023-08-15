//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.github.topi314.lavasrc.spotify;

import java.io.Serializable;
import java.time.Instant;

public class SpotifyCredentials implements Serializable {
    private final String clientID;
    private final String clientSecret;
    private final String countryCode;
    private String token;
    private Instant tokenExpire;

    public SpotifyCredentials(String clientID, String clientSecret, String countryCode) {
        this.clientID = clientID;
        this.clientSecret = clientSecret;
        if (countryCode != null && !countryCode.isEmpty()) {
            this.countryCode = countryCode;
        } else {
            this.countryCode = "US";
        }
    }

    public String getClientID() {
        return this.clientID;
    }

    public String getClientSecret() {
        return this.clientSecret;
    }

    public String getCountryCode() {
        return this.countryCode;
    }

    public String getToken() {
        return this.token;
    }

    public Instant getTokenExpire() {
        return this.tokenExpire;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setTokenExpire(Instant tokenExpire) {
        this.tokenExpire = tokenExpire;
    }
}
