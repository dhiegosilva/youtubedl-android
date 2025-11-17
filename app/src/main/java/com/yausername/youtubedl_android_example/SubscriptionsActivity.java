package com.yausername.youtubedl_android_example;

import java.util.List;

public class SubscriptionsActivity extends VideoListActivity {
    @Override
    protected List<YouTubeApiService.VideoItem> fetchVideos(YouTubeApiService service) throws Exception {
        return service.getSubscriptions();
    }
}

