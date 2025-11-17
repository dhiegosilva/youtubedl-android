package com.yausername.youtubedl_android_example;

import android.util.Log;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemListResponse;
import com.google.api.services.youtube.model.Subscription;
import com.google.api.services.youtube.model.SubscriptionListResponse;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class YouTubeApiService {
    private static final String TAG = "YouTubeApiService";
    private YouTube youtubeService;
    
    public YouTubeApiService(YouTube youtubeService) {
        this.youtubeService = youtubeService;
    }
    
    public List<VideoItem> getSubscriptions() throws IOException {
        List<VideoItem> videos = new ArrayList<>();
        try {
            // Optimized: Limit to top 10 subscriptions to reduce API calls
            // This reduces from potentially 51 calls (1 + 50 subscriptions) to 11 calls (1 + 10 subscriptions)
            YouTube.Subscriptions.List request = youtubeService.subscriptions()
                    .list(Arrays.asList("snippet", "contentDetails"))
                    .setMine(true)
                    .setMaxResults(10L); // Reduced from 50 to 10 subscriptions
            
            SubscriptionListResponse response = request.execute();
            List<Subscription> subscriptions = response.getItems();
            
            if (subscriptions != null) {
                for (Subscription subscription : subscriptions) {
                    String channelId = subscription.getSnippet().getResourceId().getChannelId();
                    
                    // Get recent videos from this channel (reduced to 3 per channel to save quota)
                    YouTube.Search.List searchRequest = youtubeService.search()
                            .list(Arrays.asList("snippet"))
                            .setChannelId(channelId)
                            .setType(Arrays.asList("video"))
                            .setOrder("date")
                            .setMaxResults(3L); // Reduced from 5 to 3 videos per channel
                    
                    com.google.api.services.youtube.model.SearchListResponse searchResponse = searchRequest.execute();
                    if (searchResponse.getItems() != null) {
                        for (com.google.api.services.youtube.model.SearchResult result : searchResponse.getItems()) {
                            VideoItem item = new VideoItem();
                            item.videoId = result.getId().getVideoId();
                            item.title = result.getSnippet().getTitle();
                            item.description = result.getSnippet().getDescription();
                            if (result.getSnippet().getThumbnails() != null &&
                                result.getSnippet().getThumbnails().getDefault() != null) {
                                item.thumbnailUrl = result.getSnippet().getThumbnails().getDefault().getUrl();
                            }
                            item.channelTitle = result.getSnippet().getChannelTitle();
                            videos.add(item);
                        }
                    }
                }
            }
        } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException e) {
            Log.e(TAG, "Google API error fetching subscriptions", e);
            throw new IOException("API error: " + (e.getDetails() != null ? e.getDetails().getMessage() : e.getMessage()), e);
        }
        return videos;
    }
    
    public List<PlaylistInfo> getPlaylistList() throws IOException {
        List<PlaylistInfo> playlists = new ArrayList<>();
        try {
            YouTube.Playlists.List playlistsRequest = youtubeService.playlists()
                    .list(Arrays.asList("snippet", "contentDetails"))
                    .setMine(true)
                    .setMaxResults(50L);
            
            com.google.api.services.youtube.model.PlaylistListResponse playlistsResponse = playlistsRequest.execute();
            
            if (playlistsResponse.getItems() != null) {
                for (com.google.api.services.youtube.model.Playlist playlist : playlistsResponse.getItems()) {
                    PlaylistInfo playlistInfo = new PlaylistInfo();
                    playlistInfo.playlistId = playlist.getId();
                    playlistInfo.title = playlist.getSnippet().getTitle();
                    playlistInfo.description = playlist.getSnippet().getDescription();
                    if (playlist.getSnippet().getThumbnails() != null && 
                        playlist.getSnippet().getThumbnails().getDefault() != null) {
                        playlistInfo.thumbnailUrl = playlist.getSnippet().getThumbnails().getDefault().getUrl();
                    }
                    playlistInfo.itemCount = playlist.getContentDetails() != null ? 
                        playlist.getContentDetails().getItemCount() : 0L;
                    playlists.add(playlistInfo);
                }
            }
        } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException e) {
            Log.e(TAG, "Google API error fetching playlist list", e);
            throw new IOException("API error: " + (e.getDetails() != null ? e.getDetails().getMessage() : e.getMessage()), e);
        }
        return playlists;
    }
    
    public List<VideoItem> getPlaylistVideos(String playlistId) throws IOException {
        List<VideoItem> videos = new ArrayList<>();
        try {
            YouTube.PlaylistItems.List itemsRequest = youtubeService.playlistItems()
                    .list(Arrays.asList("snippet", "contentDetails"))
                    .setPlaylistId(playlistId)
                    .setMaxResults(50L);
            
            PlaylistItemListResponse itemsResponse = itemsRequest.execute();
            if (itemsResponse.getItems() != null) {
                for (PlaylistItem item : itemsResponse.getItems()) {
                    String videoId = item.getContentDetails().getVideoId();
                    if (videoId != null) {
                        VideoItem videoItem = new VideoItem();
                        videoItem.videoId = videoId;
                        videoItem.title = item.getSnippet().getTitle();
                        videoItem.description = item.getSnippet().getDescription();
                        if (item.getSnippet().getThumbnails() != null &&
                            item.getSnippet().getThumbnails().getDefault() != null) {
                            videoItem.thumbnailUrl = item.getSnippet().getThumbnails().getDefault().getUrl();
                        }
                        videoItem.channelTitle = item.getSnippet().getChannelTitle();
                        videos.add(videoItem);
                    }
                }
            }
        } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException e) {
            Log.e(TAG, "Google API error fetching playlist videos", e);
            throw new IOException("API error: " + (e.getDetails() != null ? e.getDetails().getMessage() : e.getMessage()), e);
        }
        return videos;
    }
    
    public List<VideoItem> getRecommendations() throws IOException {
        List<VideoItem> videos = new ArrayList<>();
        try {
            YouTube.Activities.List request = youtubeService.activities()
                    .list(Arrays.asList("snippet", "contentDetails"))
                    .setHome(true)
                    .setMaxResults(50L);
            
            com.google.api.services.youtube.model.ActivityListResponse response = request.execute();
            
            if (response.getItems() != null) {
                for (com.google.api.services.youtube.model.Activity activity : response.getItems()) {
                    if (activity.getContentDetails() != null && 
                        activity.getContentDetails().getUpload() != null) {
                        String videoId = activity.getContentDetails().getUpload().getVideoId();
                        if (videoId != null) {
                            VideoItem videoItem = new VideoItem();
                            videoItem.videoId = videoId;
                            videoItem.title = activity.getSnippet().getTitle();
                            videoItem.description = activity.getSnippet().getDescription();
                            if (activity.getSnippet().getThumbnails() != null &&
                                activity.getSnippet().getThumbnails().getDefault() != null) {
                                videoItem.thumbnailUrl = activity.getSnippet().getThumbnails().getDefault().getUrl();
                            }
                            videoItem.channelTitle = activity.getSnippet().getChannelTitle();
                            videos.add(videoItem);
                        }
                    }
                }
            }
        } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException e) {
            Log.e(TAG, "Google API error fetching recommendations", e);
            throw new IOException("API error: " + (e.getDetails() != null ? e.getDetails().getMessage() : e.getMessage()), e);
        }
        return videos;
    }
    
    public static class VideoItem {
        public String videoId;
        public String title;
        public String description;
        public String thumbnailUrl;
        public String channelTitle;
        public String playlistName;
        
        public String getVideoUrl() {
            return "https://www.youtube.com/watch?v=" + videoId;
        }
    }
    
    public static class PlaylistInfo {
        public String playlistId;
        public String title;
        public String description;
        public String thumbnailUrl;
        public Long itemCount;
    }
}

