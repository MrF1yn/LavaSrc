package com.github.topi314.lavasrc.spotify;

import com.github.topi314.lavasearch.AudioSearchSourceManager;
import com.github.topi314.lavasearch.result.AudioSearchResult;
import com.github.topi314.lavasearch.result.BasicAudioSearchResult;
import com.github.topi314.lavasrc.ExtendedAudioPlaylist;
import com.github.topi314.lavasrc.LavaSrcTools;
import com.github.topi314.lavasrc.mirror.DefaultMirroringAudioTrackResolver;
import com.github.topi314.lavasrc.mirror.MirroringAudioSourceManager;
import com.github.topi314.lavasrc.mirror.MirroringAudioTrackResolver;
import com.github.topi314.lavasrc.spotify.data.SpotifyArtistInfo;
import com.github.topi314.lavasrc.spotify.data.SpotifyTrackFeatures;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpConfigurable;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager;
import com.sedmelluq.discord.lavaplayer.track.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SpotifySourceManager extends MirroringAudioSourceManager implements HttpConfigurable, AudioSearchSourceManager {

	public static final Pattern URL_PATTERN = Pattern.compile("(https?://)(www\\.)?open\\.spotify\\.com/((?<region>[a-zA-Z-]+)/)?(user/(?<user>[a-zA-Z0-9-_]+)/)?(?<type>track|album|playlist|artist)/(?<identifier>[a-zA-Z0-9-_]+)");
	public static final String SEARCH_PREFIX = "spsearch:";
	public static final String RECOMMENDATIONS_PREFIX = "sprec:";
	public static final String PREVIEW_PREFIX = "spprev:";
	public static final long PREVIEW_LENGTH = 30000;
	public static final int PLAYLIST_MAX_PAGE_ITEMS = 100;
	public static final int ALBUM_MAX_PAGE_ITEMS = 50;
	public static final String API_BASE = "https://api.spotify.com/v1/";
	public static final Set<AudioSearchResult.Type> SEARCH_TYPES = Set.of(AudioSearchResult.Type.ALBUM, AudioSearchResult.Type.ARTIST, AudioSearchResult.Type.PLAYLIST, AudioSearchResult.Type.TRACK);
	private static final Logger log = LoggerFactory.getLogger(SpotifySourceManager.class);
	private final HttpInterfaceManager httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager();

	private HashMap<Long, SpotifyCredentials> GUILD_SPOTIFY = new HashMap();
	private int playlistPageLimit = 6;
	private int albumPageLimit = 6;

	public void registerSpotifyCredentials(String clientID, String clientSecret, String countryCode, long guildID) {
		if (countryCode == null || countryCode.isEmpty()) {
			countryCode = "US";
		}

		this.GUILD_SPOTIFY.put(guildID, new SpotifyCredentials(clientID, clientSecret, countryCode));
	}

	public void unregisterSpotifyCredentials(long guildID) {
		this.GUILD_SPOTIFY.remove(guildID);
	}



	public SpotifySourceManager(String[] providers, AudioPlayerManager audioPlayerManager) {
		super(audioPlayerManager, new DefaultMirroringAudioTrackResolver(providers));
	}

	public void setPlaylistPageLimit(int playlistPageLimit) {
		this.playlistPageLimit = playlistPageLimit;
	}

	public void setAlbumPageLimit(int albumPageLimit) {
		this.albumPageLimit = albumPageLimit;
	}

	@Override
	public String getSourceName() {
		return "spotify";
	}

	@Override
	public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) throws IOException {
		var extendedAudioTrackInfo = super.decodeTrack(input);
		return new SpotifyAudioTrack(trackInfo,
			extendedAudioTrackInfo.albumName,
			extendedAudioTrackInfo.albumUrl,
			extendedAudioTrackInfo.artistUrl,
			extendedAudioTrackInfo.artistArtworkUrl,
			extendedAudioTrackInfo.previewUrl,
			extendedAudioTrackInfo.isPreview,
			this
		);
	}

	@Override
	@Nullable
	public AudioSearchResult loadSearch(@NotNull String query, @NotNull Set<AudioSearchResult.Type> types) {
		String[] arr = query.split(" ");
		long guildID;
		if (arr.length < 2) {
			guildID = -1;
		} else {
			String rawGuildID = arr[0];
			query = String.join(" ", Arrays.copyOfRange(arr, 1, arr.length));

			try {
				guildID = Long.parseLong(rawGuildID);
			} catch (Exception var13) {
				guildID = -1;
			}
		}
		try {
			if (query.startsWith(SEARCH_PREFIX)) {
				return this.getAutocomplete(query.substring(SEARCH_PREFIX.length()), types, guildID);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return null;
	}

	@Override
	public AudioItem loadItem(AudioPlayerManager manager, AudioReference reference) {
		var identifier = reference.identifier;
		var preview = reference.identifier.startsWith(PREVIEW_PREFIX);
		return this.loadItem(preview ? identifier.substring(PREVIEW_PREFIX.length()) : identifier, preview);
	}

	public AudioItem loadItem(String identifier, boolean preview) {
		String[] arr = identifier.split(" ");
		if (arr.length < 2) {
			return null;
		} else {
			String rawGuildID = arr[0];
			identifier = String.join(" ", Arrays.copyOfRange(arr, 1, arr.length));

			long guildID;
			try {
				guildID = Long.parseLong(rawGuildID);
			} catch (Exception var13) {
				return null;
			}
			try {
				if (identifier.startsWith(SEARCH_PREFIX)) {
					return this.getSearch(identifier.substring(SEARCH_PREFIX.length()).trim(), preview, guildID);
				}

				if (identifier.startsWith(RECOMMENDATIONS_PREFIX)) {
					return this.getRecommendations(identifier.substring(RECOMMENDATIONS_PREFIX.length()).trim(), preview, guildID);
				}

				var matcher = URL_PATTERN.matcher(identifier);
				if (!matcher.find()) {
					return null;
				}

				var id = matcher.group("identifier");
				switch (matcher.group("type")) {
					case "album":
						return this.getAlbum(id, preview, guildID);

					case "track":
						return this.getTrack(id, preview, guildID);

					case "playlist":
						return this.getPlaylist(id, preview, guildID);

					case "artist":
						return this.getArtist(id, preview, guildID);
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return null;
	}

	public void requestToken(long guildID) throws IOException {
		SpotifyCredentials creds = (SpotifyCredentials)this.GUILD_SPOTIFY.get(guildID);
		if (creds == null) {
			creds = (SpotifyCredentials)this.GUILD_SPOTIFY.get(-1L);
		}

		var request = new HttpPost("https://accounts.spotify.com/api/token");
		request.addHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString((creds.getClientID() + ":" + creds.getClientSecret()).getBytes(StandardCharsets.UTF_8)));
		request.setEntity(new UrlEncodedFormEntity(List.of(new BasicNameValuePair("grant_type", "client_credentials")), StandardCharsets.UTF_8));

		var json = LavaSrcTools.fetchResponseAsJson(this.httpInterfaceManager.getInterface(), request);
		creds.setToken(json.get("access_token").text());
		creds.setTokenExpire(Instant.now().plusSeconds(json.get("expires_in").asLong(0L)));
	}

	public String getToken(long guildID) throws IOException {
		SpotifyCredentials creds = (SpotifyCredentials)this.GUILD_SPOTIFY.get(guildID);
		if (creds == null) {
			creds = (SpotifyCredentials)this.GUILD_SPOTIFY.get(-1L);
		}

		if (creds.getToken() == null || creds.getTokenExpire() == null || creds.getTokenExpire().isBefore(Instant.now())) {
			this.requestToken(guildID);
		}

		return creds.getToken();
	}

	public JsonBrowser getJson(String uri, long guildID) throws IOException {
		var request = new HttpGet(uri);
		String token = this.getToken(guildID);
		request.addHeader("Authorization", "Bearer " + token);
		return LavaSrcTools.fetchResponseAsJson(this.httpInterfaceManager.getInterface(), request);
	}

	private AudioSearchResult getAutocomplete(String query, Set<AudioSearchResult.Type> types, long guildID) throws IOException {
		if (types.contains(AudioSearchResult.Type.TEXT)) {
			throw new IllegalArgumentException("text is not a valid search type for Spotify");
		}
		if (types.isEmpty()) {
			types = SEARCH_TYPES;
		}
		var url = API_BASE + "search?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&type=" + types.stream().map(AudioSearchResult.Type::name).collect(Collectors.joining(","));
		var json = this.getJson(url, guildID);
		if (json == null) {
			return AudioSearchResult.EMPTY;
		}

		var albums = new ArrayList<AudioPlaylist>();
		for (var album : json.get("albums").get("items").values()) {
			albums.add(new SpotifyAudioPlaylist(
				album.get("name").text(),
				null,
				ExtendedAudioPlaylist.Type.ALBUM,
				album.get("external_urls").get("spotify").text(),
				album.get("images").index(0).get("url").text(),
				album.get("artists").index(0).get("name").text(),
				(int) album.get("total_tracks").asLong(0)
			));
		}

		var artists = new ArrayList<AudioPlaylist>();
		for (var artist : json.get("artists").get("items").values()) {
			artists.add(new SpotifyAudioPlaylist(
				artist.get("name").text(),
				null,
				ExtendedAudioPlaylist.Type.ARTIST,
				artist.get("external_urls").get("spotify").text(),
				artist.get("images").index(0).get("url").text(),
				null,
				null
			));
		}

		var playlists = new ArrayList<AudioPlaylist>();
		for (var playlist : json.get("playlists").get("items").values()) {
			playlists.add(new SpotifyAudioPlaylist(
				playlist.get("name").text(),
				null,
				ExtendedAudioPlaylist.Type.PLAYLIST,
				playlist.get("external_urls").get("spotify").text(),
				playlist.get("images").index(0).get("url").text(),
				null,
				(int) playlist.get("tracks").get("total").asLong(0)
			));
		}

		var tracks = this.parseTrackItems(json.get("tracks"), false);

		return new BasicAudioSearchResult(tracks, albums, artists, playlists, new ArrayList<>());
	}

	public AudioItem getSearch(String query, boolean preview, long guildID) throws IOException {
		var json = this.getJson(API_BASE + "search?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&type=track", guildID);
		if (json == null || json.get("tracks").get("items").values().isEmpty()) {
			return AudioReference.NO_TRACK;
		}

		return new BasicAudioPlaylist("Search results for: " + query, this.parseTrackItems(json.get("tracks"), preview), null, true);
	}

	public AudioItem getRecommendations(String query, boolean preview, long guildID) throws IOException {
		var json = this.getJson(API_BASE + "recommendations?" + query, guildID);
		if (json == null || json.get("tracks").values().isEmpty()) {
			return AudioReference.NO_TRACK;
		}

		return new SpotifyAudioPlaylist("Spotify Recommendations:", this.parseTracks(json, preview), ExtendedAudioPlaylist.Type.RECOMMENDATIONS, null, null, null, null);
	}

	public AudioItem getAlbum(String id, boolean preview, long guildID) throws IOException {
		var json = this.getJson(API_BASE + "albums/" + id, guildID);
		if (json == null) {
			return AudioReference.NO_TRACK;
		}

		var tracks = new ArrayList<AudioTrack>();
		JsonBrowser page;
		var offset = 0;
		var pages = 0;
		do {
			page = this.getJson(API_BASE + "albums/" + id + "/tracks?limit=" + ALBUM_MAX_PAGE_ITEMS + "&offset=" + offset, guildID);
			offset += ALBUM_MAX_PAGE_ITEMS;

			var tracksPage = this.getJson(API_BASE + "tracks/?ids=" + page.get("items").values().stream().map(track -> track.get("id").text()).collect(Collectors.joining(",")), guildID);

			for (var track : tracksPage.get("tracks").values()) {
				var albumJson = JsonBrowser.newMap();
				albumJson.put("name", json.get("name"));
				track.put("album", albumJson);

				var artistsJson = JsonBrowser.newList();
				artistsJson.add(json.get("artists").index(0));
			}
			tracks.addAll(this.parseTracks(tracksPage, preview));
		}
		while (page.get("next").text() != null && ++pages < this.albumPageLimit);

		if (tracks.isEmpty()) {
			return AudioReference.NO_TRACK;
		}

		return new SpotifyAudioPlaylist(json.get("name").text(), tracks, ExtendedAudioPlaylist.Type.ALBUM, json.get("external_urls").get("spotify").text(), json.get("images").index(0).get("url").text(), json.get("artists").index(0).get("name").text(), (int) json.get("total_tracks").asLong(0));

	}

	public AudioItem getPlaylist(String id, boolean preview, long guildID) throws IOException {
		var json = this.getJson(API_BASE + "playlists/" + id, guildID);
		if (json == null) {
			return AudioReference.NO_TRACK;
		}

		var tracks = new ArrayList<AudioTrack>();
		JsonBrowser page;
		var offset = 0;
		var pages = 0;
		do {
			page = this.getJson(API_BASE + "playlists/" + id + "/tracks?limit=" + PLAYLIST_MAX_PAGE_ITEMS + "&offset=" + offset, guildID);
			offset += PLAYLIST_MAX_PAGE_ITEMS;

			for (var value : page.get("items").values()) {
				var track = value.get("track");
				if (track.isNull() || track.get("is_local").asBoolean(false)) {
					continue;
				}
				tracks.add(this.parseTrack(track, preview));
			}

		}
		while (page.get("next").text() != null && ++pages < this.playlistPageLimit);

		if (tracks.isEmpty()) {
			return AudioReference.NO_TRACK;
		}

		return new SpotifyAudioPlaylist(json.get("name").text(), tracks, ExtendedAudioPlaylist.Type.PLAYLIST, json.get("external_urls").get("spotify").text(), json.get("images").index(0).get("url").text(), json.get("owner").get("display_name").text(), (int) json.get("tracks").get("total").asLong(0));

	}

	public AudioItem getArtist(String id, boolean preview, long guildID) throws IOException {
		SpotifyCredentials creds = GUILD_SPOTIFY.get(guildID);
		if (creds==null){
			creds = GUILD_SPOTIFY.get(-1L);
		}
		var json = this.getJson(API_BASE + "artists/" + id + "/top-tracks?market=" + creds.getCountryCode(), guildID);
		if (json == null || json.get("tracks").values().isEmpty()) {
			return AudioReference.NO_TRACK;
		}
		return new SpotifyAudioPlaylist(json.get("tracks").index(0).get("artists").index(0).get("name").text() + "'s Top Tracks", this.parseTracks(json, preview), ExtendedAudioPlaylist.Type.ARTIST, json.get("tracks").index(0).get("external_urls").get("spotify").text(), json.get("tracks").index(0).get("album").get("images").index(0).get("url").text(), json.get("tracks").index(0).get("artists").index(0).get("name").text(), (int) json.get("tracks").get("total").asLong(0));
	}

	public SpotifyArtistInfo getArtistInfo(String id, long guildID) throws IOException {
		String countryCode = "US";
		if (this.GUILD_SPOTIFY.containsKey(guildID))
			countryCode = this.GUILD_SPOTIFY.get(guildID).getCountryCode();
		JsonBrowser json = getJson("https://api.spotify.com/v1/artists/" + id, guildID);
		if (json == null)
			return null;
		return json.as(SpotifyArtistInfo.class);
	}

	public SpotifyTrackFeatures getTrackFeatures(String id, long guildID) throws IOException {
		String countryCode = "US";
		if (this.GUILD_SPOTIFY.containsKey(guildID)) {
			countryCode = this.GUILD_SPOTIFY.get(guildID).getCountryCode();
		}

		JsonBrowser json = this.getJson("https://api.spotify.com/v1/audio-features/" + id, guildID);
		return json == null ? null : json.as(SpotifyTrackFeatures.class);
	}

	public AudioItem getTrack(String id, boolean preview, long guildID) throws IOException {
		var json = this.getJson(API_BASE + "tracks/" + id, guildID);
		if (json == null) {
			return AudioReference.NO_TRACK;
		}
		return this.parseTrack(json, preview);
	}

	private List<AudioTrack> parseTracks(JsonBrowser json, boolean preview) {
		var tracks = new ArrayList<AudioTrack>();
		for (var value : json.get("tracks").values()) {
			tracks.add(this.parseTrack(value, preview));
		}
		return tracks;
	}

	private List<AudioTrack> parseTrackItems(JsonBrowser json, boolean preview) {
		var tracks = new ArrayList<AudioTrack>();
		for (var value : json.get("items").values()) {
			if (value.get("is_local").asBoolean(false)) {
				continue;
			}
			tracks.add(this.parseTrack(value, preview));
		}
		return tracks;
	}

	private AudioTrack parseTrack(JsonBrowser json, boolean preview) {
		return new SpotifyAudioTrack(
			new AudioTrackInfo(
				json.get("name").text(),
				json.get("artists").index(0).get("name").text(),
				preview ? PREVIEW_LENGTH : json.get("duration_ms").asLong(0),
				json.get("id").text(),
				false,
				json.get("external_urls").get("spotify").text(),
				json.get("album").get("images").index(0).get("url").text(),
				json.get("external_ids").get("isrc").text()
			),
			json.get("album").get("name").text(),
			json.get("album").get("external_urls").get("spotify").text(),
			json.get("artists").index(0).get("external_urls").get("spotify").text(),
			json.get("artists").index(0).get("images").index(0).get("url").text(),
			json.get("preview_url").text(),
			preview,
			this
		);
	}

	@Override
	public void shutdown() {
		try {
			this.httpInterfaceManager.close();
		} catch (IOException e) {
			log.error("Failed to close HTTP interface manager", e);
		}
	}

	@Override
	public void configureRequests(Function<RequestConfig, RequestConfig> configurator) {
		this.httpInterfaceManager.configureRequests(configurator);
	}

	@Override
	public void configureBuilder(Consumer<HttpClientBuilder> configurator) {
		this.httpInterfaceManager.configureBuilder(configurator);
	}

}
