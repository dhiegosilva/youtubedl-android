package com.yausername.youtubedl_android_example;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class OAuthConfig {
    private static final String TAG = "OAuthConfig";
    private static final String PROPERTIES_FILE = "oauth.properties";
    
    private static String clientId = null;
    private static String clientSecret = null;
    
    public static String getClientId(Context context) {
        if (clientId == null) {
            loadProperties(context);
        }
        return clientId;
    }
    
    public static String getClientSecret(Context context) {
        if (clientSecret == null) {
            loadProperties(context);
        }
        return clientSecret;
    }
    
    private static void loadProperties(Context context) {
        try {
            Properties properties = new Properties();
            InputStream inputStream = context.getAssets().open(PROPERTIES_FILE);
            properties.load(inputStream);
            inputStream.close();
            
            clientId = properties.getProperty("oauth.client.id");
            clientSecret = properties.getProperty("oauth.client.secret");
            
            if (clientId == null || clientSecret == null) {
                Log.e(TAG, "OAuth credentials not found in properties file");
                throw new RuntimeException("OAuth credentials not configured. Please create app/src/main/assets/oauth.properties");
            }
            
            Log.d(TAG, "OAuth credentials loaded successfully");
        } catch (IOException e) {
            Log.e(TAG, "Error loading OAuth properties", e);
            throw new RuntimeException("Failed to load OAuth credentials. Please create app/src/main/assets/oauth.properties", e);
        }
    }
}

