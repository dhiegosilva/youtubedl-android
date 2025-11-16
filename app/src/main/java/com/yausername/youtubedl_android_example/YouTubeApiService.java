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
    
    public List<VideoItem> getSubscriptions() {
        List<VideoItem> videos = new ArrayList<>();
        try {
            YouTube.Subscriptions.List request = youtubeService.subscriptions()
                    .list(Arrays.asList("snippet", "contentDetails"))
                    .setMine(true)
                    .setMaxResults(50L);
            
            SubscriptionListResponse response = request.execute();
            List<Subscription> subscriptions = response.getItems();
            
            for (Subscription subscription : subscriptions) {
                String channelId = subscription.getSnippet().getResourceId().getChannelId();
                
                // Get recent videos from this channel
                YouTube.Search.List searchRequest = youtubeService.search()
                        .list(Arrays.asList("snippet"))
                        .setChannelId(channelId)
                        .setType(Arrays.asList("video"))
                        .setOrder("date")
                        .setMaxResults(5L);
                
                com.google.api.services.youtube.model.SearchListResponse searchResponse = searchRequest.execute();
                for (com.google.api.services.youtube.model.SearchResult result : searchResponse.getItems()) {
                    VideoItem item = new VideoItem();
                    item.videoId = result.getId().getVideoId();
                    item.title = result.getSnippet().getTitle();
                    item.description = result.getSnippet().getDescription();
                    item.thumbnailUrl = result.getSnippet().getThumbnails().getDefault().getUrl();
                    item.channelTitle = result.getSnippet().getChannelTitle();
                    videos.add(item);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error fetching subscriptions", e);
        }
        return videos;
    }
    
    public List<VideoItem> getPlaylists() {
        List<VideoItem> videos = new ArrayList<>();
        try {
            YouTube.Playlists.List playlistsRequest = youtubeService.playlists()
                    .list(Arrays.asList("snippet", "contentDetails"))
                    .setMine(true)
                    .setMaxResults(50L);
            
            com.google.api.services.youtube.model.PlaylistListResponse playlistsResponse = playlistsRequest.execute();
            
            for (com.google.api.services.youtube.model.Playlist playlist : playlistsResponse.getItems()) {
                String playlistId = playlist.getId();
                
                YouTube.PlaylistItems.List itemsRequest = youtubeService.playlistItems()
                        .list(Arrays.asList("snippet", "contentDetails"))
                        .setPlaylistId(playlistId)
                        .setMaxResults(50L);
                
                PlaylistItemListResponse itemsResponse = itemsRequest.execute();
                for (PlaylistItem item : itemsResponse.getItems()) {
                    String videoId = item.getContentDetails().getVideoId();
                    if (videoId != null) {
                        VideoItem videoItem = new VideoItem();
                        videoItem.videoId = videoId;
                        videoItem.title = item.getSnippet().getTitle();
                        videoItem.description = item.getSnippet().getDescription();
                        videoItem.thumbnailUrl = item.getSnippet().getThumbnails().getDefault().getUrl();
                        videoItem.channelTitle = item.getSnippet().getChannelTitle();
                        videoItem.playlistName = playlist.getSnippet().getTitle();
                        videos.add(videoItem);
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error fetching playlists", e);
        }
        return videos;
    }
    
    public List<VideoItem> getRecommendations() {
        List<VideoItem> videos = new ArrayList<>();
        try {
            YouTube.Activities.List request = youtubeService.activities()
                    .list(Arrays.asList("snippet", "contentDetails"))
                    .setHome(true)
                    .setMaxResults(50L);
            
            com.google.api.services.youtube.model.ActivityListResponse response = request.execute();
            
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
        } catch (IOException e) {
            Log.e(TAG, "Error fetching recommendations", e);
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
}

