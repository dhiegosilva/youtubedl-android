package com.yausername.youtubedl_android_example;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.api.client.http.HttpResponseException;

import java.io.IOException;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    
    private Button btnSignIn;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private TextView tvUserCode;
    private TextView tvInstructions;
    private GoogleAuthHelper authHelper;
    private DeviceCodeAuth deviceCodeAuth;
    private Handler handler;
    private boolean isPolling = false;
    private long expirationTime = 0;
    private Runnable expirationRunnable;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        authHelper = new GoogleAuthHelper(this);
        deviceCodeAuth = new DeviceCodeAuth(this);
        handler = new Handler(Looper.getMainLooper());
        
        initViews();
        
        if (authHelper.isLoggedIn()) {
            showLoggedInState();
        }
    }
    
    private void initViews() {
        btnSignIn = findViewById(R.id.btn_sign_in);
        progressBar = findViewById(R.id.progress_bar);
        tvStatus = findViewById(R.id.tv_status);
        tvUserCode = findViewById(R.id.tv_user_code);
        tvInstructions = findViewById(R.id.tv_instructions);
        
        btnSignIn.setOnClickListener(v -> {
            if (authHelper.isLoggedIn()) {
                signOut();
            } else {
                startDeviceCodeFlow();
            }
        });
    }
    
    private void startDeviceCodeFlow() {
        progressBar.setVisibility(View.VISIBLE);
        btnSignIn.setEnabled(false);
        tvStatus.setText("Requesting device code...");
        tvUserCode.setVisibility(View.GONE);
        tvInstructions.setVisibility(View.GONE);
        
        new Thread(() -> {
            try {
                Log.d(TAG, "Requesting device code...");
                DeviceCodeAuth.DeviceCodeResponse deviceCodeResponse = deviceCodeAuth.requestDeviceCode();
                
                if (deviceCodeResponse == null || deviceCodeResponse.user_code == null) {
                    Log.e(TAG, "Device code response is null or invalid");
                    handler.post(() -> {
                        progressBar.setVisibility(View.GONE);
                        tvStatus.setText("Error: Invalid response from server");
                        btnSignIn.setEnabled(true);
                        btnSignIn.setText("Try Again");
                        Toast.makeText(this, "Failed to get device code", Toast.LENGTH_LONG).show();
                    });
                    return;
                }
                
                Log.d(TAG, "Device code received: " + deviceCodeResponse.user_code);
                Log.d(TAG, "Expires in: " + deviceCodeResponse.expires_in + " seconds");
                Log.d(TAG, "Poll interval: " + deviceCodeResponse.interval + " seconds");
                
                expirationTime = System.currentTimeMillis() + (deviceCodeResponse.expires_in * 1000L);
                
                handler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    tvStatus.setText("Enter this code on your device:");
                    tvUserCode.setText(deviceCodeResponse.user_code);
                    tvUserCode.setVisibility(View.VISIBLE);
                    tvInstructions.setText("Go to: google.com/device\n\nEnter the code above to authorize this app.\n\nCode expires in " + deviceCodeResponse.expires_in + " seconds.");
                    tvInstructions.setVisibility(View.VISIBLE);
                    btnSignIn.setText("Checking authorization...");
                    btnSignIn.setEnabled(false);
                    
                    // Start expiration countdown
                    startExpirationCountdown(deviceCodeResponse.expires_in);
                    
                    // Start polling for token
                    startPolling(deviceCodeResponse.device_code, deviceCodeResponse.interval);
                });
            } catch (Exception e) {
                Log.e(TAG, "Error requesting device code", e);
                handler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    tvStatus.setText("Error: " + e.getMessage());
                    btnSignIn.setEnabled(true);
                    btnSignIn.setText("Try Again");
                    Toast.makeText(this, "Failed to request device code: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
    
    private void startExpirationCountdown(int expiresIn) {
        if (expirationRunnable != null) {
            handler.removeCallbacks(expirationRunnable);
        }
        
        expirationRunnable = new Runnable() {
            @Override
            public void run() {
                long remaining = (expirationTime - System.currentTimeMillis()) / 1000;
                if (remaining > 0) {
                    tvInstructions.setText("Go to: google.com/device\n\nEnter the code above to authorize this app.\n\nCode expires in " + remaining + " seconds.");
                    handler.postDelayed(this, 1000);
                } else {
                    // Expired
                    isPolling = false;
                    tvStatus.setText("Device code expired. Please try again.");
                    tvUserCode.setVisibility(View.GONE);
                    tvInstructions.setVisibility(View.GONE);
                    btnSignIn.setEnabled(true);
                    btnSignIn.setText("Try Again");
                    Toast.makeText(LoginActivity.this, "Device code expired", Toast.LENGTH_LONG).show();
                }
            }
        };
        handler.post(expirationRunnable);
    }
    
    private void startPolling(String deviceCode, int intervalSeconds) {
        if (isPolling) {
            Log.w(TAG, "Already polling, ignoring new request");
            return;
        }
        
        isPolling = true;
        final int pollInterval = Math.max(intervalSeconds, 5) * 1000; // Convert to milliseconds, minimum 5 seconds
        
        Log.d(TAG, "Starting polling with interval: " + pollInterval + "ms");
        
        new Thread(() -> {
            // Wait before first poll
            try {
                Thread.sleep(pollInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                isPolling = false;
                return;
            }
            
            while (isPolling) {
                // Check if expired
                if (System.currentTimeMillis() >= expirationTime) {
                    Log.d(TAG, "Device code expired, stopping polling");
                    handler.post(() -> {
                        isPolling = false;
                        tvStatus.setText("Device code expired. Please try again.");
                        tvUserCode.setVisibility(View.GONE);
                        tvInstructions.setVisibility(View.GONE);
                        btnSignIn.setEnabled(true);
                        btnSignIn.setText("Try Again");
                        if (expirationRunnable != null) {
                            handler.removeCallbacks(expirationRunnable);
                        }
                    });
                    return;
                }
                
                try {
                    Log.d(TAG, "Polling for token...");
                    DeviceCodeAuth.TokenResponse tokenResponse = deviceCodeAuth.pollForToken(deviceCode, intervalSeconds);
                    
                    if (tokenResponse == null) {
                        Log.e(TAG, "Token response is null");
                        Thread.sleep(pollInterval);
                        continue;
                    }
                    
                    if (tokenResponse.access_token != null && !tokenResponse.access_token.isEmpty()) {
                        // Success!
                        Log.d(TAG, "Access token received!");
                        handler.post(() -> {
                            authHelper.saveTokens(tokenResponse.access_token, tokenResponse.refresh_token);
                            isPolling = false;
                            if (expirationRunnable != null) {
                                handler.removeCallbacks(expirationRunnable);
                            }
                            showLoggedInState();
                            Toast.makeText(this, "Successfully logged in!", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        });
                        return;
                    } else if ("authorization_pending".equals(tokenResponse.error)) {
                        // Still waiting for user authorization
                        Log.d(TAG, "Authorization pending, continuing to poll...");
                        // Continue polling
                    } else if ("slow_down".equals(tokenResponse.error)) {
                        // Need to slow down polling
                        Log.d(TAG, "Slow down requested, increasing interval");
                        Thread.sleep(pollInterval * 2);
                        continue;
                    } else if ("expired_token".equals(tokenResponse.error)) {
                        // Token expired
                        Log.d(TAG, "Device code expired");
                        handler.post(() -> {
                            isPolling = false;
                            if (expirationRunnable != null) {
                                handler.removeCallbacks(expirationRunnable);
                            }
                            tvStatus.setText("Device code expired. Please try again.");
                            tvUserCode.setVisibility(View.GONE);
                            tvInstructions.setVisibility(View.GONE);
                            btnSignIn.setEnabled(true);
                            btnSignIn.setText("Try Again");
                            Toast.makeText(this, "Device code expired", Toast.LENGTH_LONG).show();
                        });
                        return;
                    } else {
                        // Other error
                        Log.e(TAG, "Error in token response: " + tokenResponse.error + " - " + tokenResponse.error_description);
                        handler.post(() -> {
                            isPolling = false;
                            if (expirationRunnable != null) {
                                handler.removeCallbacks(expirationRunnable);
                            }
                            progressBar.setVisibility(View.GONE);
                            tvStatus.setText("Error: " + (tokenResponse.error_description != null ? tokenResponse.error_description : tokenResponse.error));
                            btnSignIn.setEnabled(true);
                            btnSignIn.setText("Try Again");
                            Toast.makeText(this, "Authorization failed: " + tokenResponse.error, Toast.LENGTH_LONG).show();
                        });
                        return;
                    }
                    
                    // Wait before next poll
                    Thread.sleep(pollInterval);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    isPolling = false;
                    return;
                } catch (HttpResponseException e) {
                    Log.e(TAG, "HTTP error polling for token: " + e.getStatusCode() + " - " + e.getMessage());
                    if (e.getStatusCode() == 400) {
                        // Bad request, might be expired
                        String content = "";
                        try {
                            content = e.getContent();
                        } catch (Exception ex) {
                            // Ignore
                        }
                        Log.e(TAG, "Response content: " + content);
                        handler.post(() -> {
                            isPolling = false;
                            if (expirationRunnable != null) {
                                handler.removeCallbacks(expirationRunnable);
                            }
                            progressBar.setVisibility(View.GONE);
                            tvStatus.setText("Device code expired or invalid. Please try again.");
                            tvUserCode.setVisibility(View.GONE);
                            tvInstructions.setVisibility(View.GONE);
                            btnSignIn.setEnabled(true);
                            btnSignIn.setText("Try Again");
                        });
                        return;
                    }
                    // For other HTTP errors, continue polling
                    try {
                        Thread.sleep(pollInterval);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        isPolling = false;
                        return;
                    }
                } catch (IOException e) {
                    Log.e(TAG, "IO error polling for token", e);
                    // Continue polling on network errors
                    try {
                        Thread.sleep(pollInterval);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        isPolling = false;
                        return;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Unexpected error polling for token", e);
                    // Continue polling
                    try {
                        Thread.sleep(pollInterval);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        isPolling = false;
                        return;
                    }
                }
            }
        }).start();
    }
    
    private void showLoggedInState() {
        tvStatus.setText("Logged in");
        tvUserCode.setVisibility(View.GONE);
        tvInstructions.setVisibility(View.GONE);
        btnSignIn.setText("Sign Out");
        btnSignIn.setEnabled(true);
    }
    
    private void signOut() {
        isPolling = false;
        authHelper.clearTokens();
        tvStatus.setText("Not logged in");
        tvUserCode.setVisibility(View.GONE);
        tvInstructions.setVisibility(View.GONE);
        btnSignIn.setText("Sign In with Google");
        Toast.makeText(this, "Signed out", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    protected void onDestroy() {
        isPolling = false;
        if (expirationRunnable != null) {
            handler.removeCallbacks(expirationRunnable);
        }
        super.onDestroy();
    }
}
