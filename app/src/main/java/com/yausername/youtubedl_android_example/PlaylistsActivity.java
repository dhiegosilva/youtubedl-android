package com.yausername.youtubedl_android_example;

import java.util.List;

public class PlaylistsActivity extends VideoListActivity {
    @Override
    protected List<YouTubeApiService.VideoItem> fetchVideos(YouTubeApiService service) {
        return service.getPlaylists();
    }
}

