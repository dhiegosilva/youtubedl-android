package com.yausername.youtubedl_android_example;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {
    private List<YouTubeApiService.VideoItem> videos;
    private OnVideoClickListener listener;
    private static final ExecutorService diffExecutor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final int DIFF_UTIL_THRESHOLD = 50; // Use DiffUtil only for lists > 50 items
    
    public interface OnVideoClickListener {
        void onVideoClick(YouTubeApiService.VideoItem video);
    }
    
    public VideoAdapter(List<YouTubeApiService.VideoItem> videos, OnVideoClickListener listener) {
        this.videos = videos != null ? new ArrayList<>(videos) : new ArrayList<>();
        this.listener = listener;
    }
    
    public void updateVideos(List<YouTubeApiService.VideoItem> newVideos) {
        if (newVideos == null) {
            newVideos = new ArrayList<>();
        }
        
        final List<YouTubeApiService.VideoItem> oldVideos = this.videos;
        final List<YouTubeApiService.VideoItem> finalNewVideos = new ArrayList<>(newVideos);
        
        // For small lists, use simple update to avoid overhead
        if (oldVideos.size() < DIFF_UTIL_THRESHOLD && finalNewVideos.size() < DIFF_UTIL_THRESHOLD) {
            this.videos = finalNewVideos;
            notifyDataSetChanged();
            return;
        }
        
        // Calculate DiffUtil on background thread to avoid blocking main thread
        diffExecutor.execute(() -> {
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new VideoDiffCallback(oldVideos, finalNewVideos), true);
            mainHandler.post(() -> {
                this.videos = finalNewVideos;
                diffResult.dispatchUpdatesTo(this);
            });
        });
    }
    
    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_video, parent, false);
        return new VideoViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        YouTubeApiService.VideoItem video = videos.get(position);
        holder.bind(video, listener);
    }
    
    @Override
    public void onViewRecycled(@NonNull VideoViewHolder holder) {
        super.onViewRecycled(holder);
        // Cancel any pending image loads when view is recycled
        Picasso.get().cancelRequest(holder.thumbnail);
    }
    
    @Override
    public int getItemCount() {
        return videos.size();
    }
    
    static class VideoViewHolder extends RecyclerView.ViewHolder {
        private ImageView thumbnail;
        private TextView title;
        private TextView channel;
        private TextView playlist;
        
        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.thumbnail);
            title = itemView.findViewById(R.id.title);
            channel = itemView.findViewById(R.id.channel);
            playlist = itemView.findViewById(R.id.playlist);
        }
        
        public void bind(YouTubeApiService.VideoItem video, OnVideoClickListener listener) {
            title.setText(video.title);
            channel.setText(video.channelTitle);
            
            if (video.playlistName != null && !video.playlistName.isEmpty()) {
                playlist.setVisibility(View.VISIBLE);
                playlist.setText("Playlist: " + video.playlistName);
            } else {
                playlist.setVisibility(View.GONE);
            }
            
            // Cancel any previous image load
            Picasso.get().cancelRequest(thumbnail);
            
            if (video.thumbnailUrl != null && !video.thumbnailUrl.isEmpty()) {
                // Resize image to match view size to save memory (120dp x 90dp = ~360px x 270px at mdpi)
                // Using optimized size for memory efficiency
                Picasso.get()
                        .load(video.thumbnailUrl)
                        .resize(480, 360) // Optimized size for 120dp x 90dp views (4x mdpi)
                        .centerCrop()
                        .onlyScaleDown() // Only scale down, never up (saves CPU)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .into(thumbnail);
            } else {
                thumbnail.setImageResource(android.R.drawable.ic_menu_gallery);
            }
            
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onVideoClick(video);
                }
            });
        }
    }
    
    private static class VideoDiffCallback extends DiffUtil.Callback {
        private final List<YouTubeApiService.VideoItem> oldList;
        private final List<YouTubeApiService.VideoItem> newList;
        
        VideoDiffCallback(List<YouTubeApiService.VideoItem> oldList, List<YouTubeApiService.VideoItem> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }
        
        @Override
        public int getOldListSize() {
            return oldList.size();
        }
        
        @Override
        public int getNewListSize() {
            return newList.size();
        }
        
        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).videoId != null &&
                   oldList.get(oldItemPosition).videoId.equals(newList.get(newItemPosition).videoId);
        }
        
        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            // Simplified comparison - only check videoId for performance
            // If videoId is the same, assume content is the same (DiffUtil will handle moves/updates)
            YouTubeApiService.VideoItem oldItem = oldList.get(oldItemPosition);
            YouTubeApiService.VideoItem newItem = newList.get(newItemPosition);
            return oldItem.videoId != null && oldItem.videoId.equals(newItem.videoId);
        }
        
        @Override
        public Object getChangePayload(int oldItemPosition, int newItemPosition) {
            // Return null to indicate full update if needed, or return payload for partial updates
            return null;
        }
    }
}

