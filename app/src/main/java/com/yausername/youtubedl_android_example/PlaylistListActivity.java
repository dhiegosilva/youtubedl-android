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

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class PlaylistListActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private PlaylistAdapter adapter;
    private GoogleAuthHelper authHelper;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    
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
        adapter = new PlaylistAdapter(new ArrayList<>(), playlist -> {
            // Open playlist videos activity
            Intent intent = new Intent(this, PlaylistVideosActivity.class);
            intent.putExtra("playlist_id", playlist.playlistId);
            intent.putExtra("playlist_title", playlist.title);
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
        
        if (!authHelper.isLoggedIn()) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        loadPlaylists();
    }
    
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
    
    private void loadPlaylists() {
        progressBar.setVisibility(View.VISIBLE);
        
        Disposable disposable = Observable.fromCallable(() -> {
                    try {
                        String accessToken = authHelper.getAccessToken();
                        if (accessToken == null) {
                            throw new Exception("Not logged in");
                        }
                        
                        // Reuse cached YouTube service if available
                        YouTube youtubeService = getOrCreateYouTubeService();
                        YouTubeApiService service = new YouTubeApiService(youtubeService);
                        return service.getPlaylistList();
                    } catch (Exception e) {
                        Log.e("PlaylistListActivity", "Error loading playlists", e);
                        throw new RuntimeException(e);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(playlists -> {
                    progressBar.setVisibility(View.GONE);
                    if (playlists != null && !playlists.isEmpty()) {
                        adapter.updatePlaylists(playlists);
                        Toast.makeText(this, "Loaded " + playlists.size() + " playlist(s)", Toast.LENGTH_SHORT).show();
                    } else {
                        adapter.updatePlaylists(new ArrayList<>());
                        Toast.makeText(this, "No playlists found", Toast.LENGTH_SHORT).show();
                    }
                }, error -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e("PlaylistListActivity", "Error", error);
                    String errorMsg = error.getMessage();
                    if (error.getCause() != null) {
                        errorMsg = error.getCause().getMessage();
                    }
                    Toast.makeText(this, "Error loading playlists: " + errorMsg, Toast.LENGTH_LONG).show();
                });
        
        compositeDisposable.add(disposable);
    }
    
    @Override
    protected void onDestroy() {
        compositeDisposable.dispose();
        super.onDestroy();
    }
}

