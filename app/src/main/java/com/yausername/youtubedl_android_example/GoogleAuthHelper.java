package com.yausername.youtubedl_android_example;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeScopes;

import java.util.Collections;
import java.util.List;

public class GoogleAuthHelper {
    private static final String TAG = "GoogleAuthHelper";
    private static final List<String> SCOPES = Collections.singletonList(YouTubeScopes.YOUTUBE_READONLY);
    
    private static final com.google.api.client.json.JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String APPLICATION_NAME = "YouTube-DL Android";
    
    // Cache transport instance to avoid recreating it (expensive operation)
    private static volatile NetHttpTransport cachedTransport;
    
    private Context context;
    private SharedPreferences prefs;
    private static final String PREF_NAME = "youtube_auth";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    
    public GoogleAuthHelper(Context context) {
        this.context = context.getApplicationContext(); // Use application context to prevent leaks
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    private static NetHttpTransport getTransport() {
        if (cachedTransport == null) {
            synchronized (GoogleAuthHelper.class) {
                if (cachedTransport == null) {
                    cachedTransport = new NetHttpTransport();
                }
            }
        }
        return cachedTransport;
    }
    
    public Credential getCredential(String accessToken, String refreshToken) {
        String clientId = OAuthConfig.getClientId(context);
        String clientSecret = OAuthConfig.getClientSecret(context);
        
        GoogleCredential credential = new GoogleCredential.Builder()
                .setTransport(getTransport()) // Reuse cached transport
                .setJsonFactory(JSON_FACTORY)
                .setClientSecrets(clientId, clientSecret)
                .build();
        
        credential.setAccessToken(accessToken);
        if (refreshToken != null) {
            credential.setRefreshToken(refreshToken);
        }
        
        return credential;
    }
    
    public YouTube getYouTubeService(String accessToken, String refreshToken) {
        Credential credential = getCredential(accessToken, refreshToken);
        
        return new YouTube.Builder(
                getTransport(), // Reuse cached transport
                JSON_FACTORY,
                credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
    
    
    public void saveTokens(String accessToken, String refreshToken) {
        prefs.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .apply();
    }
    
    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }
    
    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }
    
    public void clearTokens() {
        prefs.edit()
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .apply();
    }
    
    public boolean isLoggedIn() {
        return getAccessToken() != null;
    }
    
    public String refreshAccessToken() {
        String refreshToken = getRefreshToken();
        if (refreshToken == null) {
            return null;
        }
        
        try {
            DeviceCodeAuth deviceCodeAuth = new DeviceCodeAuth(context);
            DeviceCodeAuth.TokenResponse response = deviceCodeAuth.refreshToken(refreshToken);
            if (response.access_token != null) {
                saveTokens(response.access_token, refreshToken);
                return response.access_token;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error refreshing token", e);
        }
        return null;
    }
    
    public static String getClientId(Context context) {
        return OAuthConfig.getClientId(context);
    }
}

