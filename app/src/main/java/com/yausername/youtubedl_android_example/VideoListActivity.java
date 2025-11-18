package com.yausername.youtubedl_android_example;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeScopes;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public abstract class VideoListActivity extends AppCompatActivity {
    protected RecyclerView recyclerView;
    protected ProgressBar progressBar;
    protected VideoAdapter adapter;
    protected GoogleAuthHelper authHelper;
    protected YouTubeApiService apiService;
    protected CompositeDisposable compositeDisposable = new CompositeDisposable();
    
    // Cache YouTube service to avoid recreating
    private YouTube cachedYouTubeService;
    private String cachedAccessToken;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_list);
        
        authHelper = new GoogleAuthHelper(this);
        
        recyclerView = findViewById(R.id.recycler_view);
        progressBar = findViewById(R.id.progress_bar);
        
        // Optimize RecyclerView
        recyclerView.setHasFixedSize(false); // Allow size changes for better performance with dynamic lists
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setItemViewCacheSize(20); // Cache more views to reduce layout passes
        adapter = new VideoAdapter(new ArrayList<>(), videoItem -> {
            // Play video
            Intent intent = new Intent(this, StreamingExampleActivity.class);
            intent.putExtra("video_url", videoItem.getVideoUrl());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
        
        if (!authHelper.isLoggedIn()) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        loadVideos();
    }
    
    protected abstract List<YouTubeApiService.VideoItem> fetchVideos(YouTubeApiService service) throws Exception;
    
    private YouTube getOrCreateYouTubeService() {
        String currentAccessToken = authHelper.getAccessToken();
        // Reuse service if token hasn't changed
        if (cachedYouTubeService != null && currentAccessToken != null && currentAccessToken.equals(cachedAccessToken)) {
            return cachedYouTubeService;
        }
        
        cachedAccessToken = currentAccessToken;
        cachedYouTubeService = authHelper.getYouTubeService(
                currentAccessToken,
                authHelper.getRefreshToken());
        return cachedYouTubeService;
    }
    
    private void loadVideos() {
        progressBar.setVisibility(View.VISIBLE);
        
        Disposable disposable = Observable.fromCallable(() -> {
                    try {
                        // Get the account from saved token
                        String accessToken = authHelper.getAccessToken();
                        if (accessToken == null) {
                            throw new Exception("Not logged in");
                        }
                        
                        // Reuse cached YouTube service if available
                        YouTube youtubeService = getOrCreateYouTubeService();
                        YouTubeApiService service = new YouTubeApiService(youtubeService);
                        return fetchVideos(service);
                    } catch (Exception e) {
                        Log.e("VideoListActivity", "Error loading videos", e);
                        throw new RuntimeException(e);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(videos -> {
                    progressBar.setVisibility(View.GONE);
                    if (videos != null && !videos.isEmpty()) {
                        adapter.updateVideos(videos);
                        Toast.makeText(this, "Loaded " + videos.size() + " video(s)", Toast.LENGTH_SHORT).show();
                    } else {
                        adapter.updateVideos(new ArrayList<>());
                        Toast.makeText(this, "No videos found", Toast.LENGTH_SHORT).show();
                    }
                }, error -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e("VideoListActivity", "Error", error);
                    String errorMsg = error.getMessage();
                    if (error.getCause() != null) {
                        errorMsg = error.getCause().getMessage();
                    }
                    // Check for quota exceeded
                    if (errorMsg != null && (errorMsg.contains("quota") || errorMsg.contains("quotaExceeded"))) {
                        Toast.makeText(this, "YouTube API quota exceeded.\n\nDaily quota limit reached.\nQuota resets at midnight Pacific Time.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Error loading videos: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
        
        compositeDisposable.add(disposable);
    }
    
    @Override
    protected void onDestroy() {
        compositeDisposable.dispose();
        super.onDestroy();
    }
}

