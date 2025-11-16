package com.yausername.youtubedl_android_example;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class DeviceCodeAuth {
    private static final String TAG = "DeviceCodeAuth";
    private static final String DEVICE_CODE_URL = "https://oauth2.googleapis.com/device/code";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String SCOPE = "https://www.googleapis.com/auth/youtube.readonly";
    
    private final Context context;
    
    public DeviceCodeAuth(Context context) {
        this.context = context;
    }
    
    public static class DeviceCodeResponse {
        public String device_code;
        public String user_code;
        public String verification_url;
        public int expires_in;
        public int interval;
    }
    
    public static class TokenResponse {
        public String access_token;
        public String refresh_token;
        public String token_type;
        public int expires_in;
        public String error;
        public String error_description;
    }
    
    public DeviceCodeResponse requestDeviceCode() throws IOException {
        Log.d(TAG, "Requesting device code from: " + DEVICE_CODE_URL);
        
        String clientId = OAuthConfig.getClientId(context);
        
        // Build POST data with proper URL encoding
        String postData = "client_id=" + URLEncoder.encode(clientId, "UTF-8") + 
                         "&scope=" + URLEncoder.encode(SCOPE, "UTF-8");
        byte[] postDataBytes = postData.getBytes(StandardCharsets.UTF_8);
        
        URL url = new URL(DEVICE_CODE_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
        connection.setDoOutput(true);
        
        // Write POST data
        try (OutputStream os = connection.getOutputStream()) {
            os.write(postDataBytes);
        }
        
        int statusCode = connection.getResponseCode();
        Log.d(TAG, "Response status code: " + statusCode);
        
        // Read response
        String response;
        if (statusCode == 200) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                response = sb.toString();
            }
        } else {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                response = sb.toString();
            }
            Log.e(TAG, "Error response: " + response);
            throw new IOException("Failed to get device code: HTTP " + statusCode + " - " + response);
        }
        
        Log.d(TAG, "Raw response: " + response);
        
        // Parse JSON using org.json.JSONObject (like SmartTube)
        try {
            JSONObject json = new JSONObject(response);
            
            DeviceCodeResponse deviceCodeResponse = new DeviceCodeResponse();
            deviceCodeResponse.device_code = json.getString("device_code");
            deviceCodeResponse.user_code = json.getString("user_code");
            deviceCodeResponse.verification_url = json.getString("verification_url");
            deviceCodeResponse.expires_in = json.getInt("expires_in");
            deviceCodeResponse.interval = json.getInt("interval");
            
            Log.d(TAG, "Device code received: " + deviceCodeResponse.user_code);
            Log.d(TAG, "Expires in: " + deviceCodeResponse.expires_in + " seconds");
            Log.d(TAG, "Interval: " + deviceCodeResponse.interval + " seconds");
            
            return deviceCodeResponse;
        } catch (org.json.JSONException e) {
            Log.e(TAG, "Error parsing JSON response", e);
            throw new IOException("Failed to parse device code response: " + e.getMessage() + " - Response: " + response);
        } finally {
            connection.disconnect();
        }
    }
    
    public TokenResponse pollForToken(String deviceCode, int intervalSeconds) throws IOException {
        String clientId = OAuthConfig.getClientId(context);
        String clientSecret = OAuthConfig.getClientSecret(context);
        
        // Build POST data with proper URL encoding
        String postData = "client_id=" + URLEncoder.encode(clientId, "UTF-8") + 
                         "&client_secret=" + URLEncoder.encode(clientSecret, "UTF-8") + 
                         "&device_code=" + URLEncoder.encode(deviceCode, "UTF-8") + 
                         "&grant_type=" + URLEncoder.encode("urn:ietf:params:oauth:grant-type:device_code", "UTF-8");
        byte[] postDataBytes = postData.getBytes(StandardCharsets.UTF_8);
        
        URL url = new URL(TOKEN_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
        connection.setDoOutput(true);
        
        // Write POST data
        try (OutputStream os = connection.getOutputStream()) {
            os.write(postDataBytes);
        }
        
        int statusCode = connection.getResponseCode();
        
        // Read response
        String response;
        if (statusCode == 200) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                response = sb.toString();
            }
        } else {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                response = sb.toString();
            }
        }
        
        // Parse JSON
        try {
            JSONObject json = new JSONObject(response);
            TokenResponse tokenResponse = new TokenResponse();
            
            if (json.has("access_token")) {
                tokenResponse.access_token = json.getString("access_token");
            }
            if (json.has("refresh_token")) {
                tokenResponse.refresh_token = json.getString("refresh_token");
            }
            if (json.has("token_type")) {
                tokenResponse.token_type = json.getString("token_type");
            }
            if (json.has("expires_in")) {
                tokenResponse.expires_in = json.getInt("expires_in");
            }
            if (json.has("error")) {
                tokenResponse.error = json.getString("error");
            }
            if (json.has("error_description")) {
                tokenResponse.error_description = json.getString("error_description");
            }
            
            return tokenResponse;
        } catch (org.json.JSONException e) {
            Log.e(TAG, "Error parsing token response", e);
            throw new IOException("Failed to parse token response: " + e.getMessage() + " - Response: " + response);
        } finally {
            connection.disconnect();
        }
    }
    
    public TokenResponse refreshToken(String refreshToken) throws IOException {
        String clientId = OAuthConfig.getClientId(context);
        String clientSecret = OAuthConfig.getClientSecret(context);
        
        // Build POST data with proper URL encoding
        String postData = "client_id=" + URLEncoder.encode(clientId, "UTF-8") + 
                         "&client_secret=" + URLEncoder.encode(clientSecret, "UTF-8") + 
                         "&refresh_token=" + URLEncoder.encode(refreshToken, "UTF-8") + 
                         "&grant_type=" + URLEncoder.encode("refresh_token", "UTF-8");
        byte[] postDataBytes = postData.getBytes(StandardCharsets.UTF_8);
        
        URL url = new URL(TOKEN_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
        connection.setDoOutput(true);
        
        // Write POST data
        try (OutputStream os = connection.getOutputStream()) {
            os.write(postDataBytes);
        }
        
        int statusCode = connection.getResponseCode();
        
        // Read response
        String response;
        if (statusCode == 200) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                response = sb.toString();
            }
        } else {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                response = sb.toString();
            }
            throw new IOException("Failed to refresh token: HTTP " + statusCode + " - " + response);
        }
        
        // Parse JSON
        try {
            JSONObject json = new JSONObject(response);
            TokenResponse tokenResponse = new TokenResponse();
            
            if (json.has("access_token")) {
                tokenResponse.access_token = json.getString("access_token");
            }
            if (json.has("refresh_token")) {
                tokenResponse.refresh_token = json.getString("refresh_token");
            }
            if (json.has("token_type")) {
                tokenResponse.token_type = json.getString("token_type");
            }
            if (json.has("expires_in")) {
                tokenResponse.expires_in = json.getInt("expires_in");
            }
            if (json.has("error")) {
                tokenResponse.error = json.getString("error");
            }
            if (json.has("error_description")) {
                tokenResponse.error_description = json.getString("error_description");
            }
            
            return tokenResponse;
        } catch (org.json.JSONException e) {
            Log.e(TAG, "Error parsing refresh token response", e);
            throw new IOException("Failed to parse refresh token response: " + e.getMessage() + " - Response: " + response);
        } finally {
            connection.disconnect();
        }
    }
}

