package com.yausername.youtubedl_android_example;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.List;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {
    private List<YouTubeApiService.VideoItem> videos;
    private OnVideoClickListener listener;
    
    public interface OnVideoClickListener {
        void onVideoClick(YouTubeApiService.VideoItem video);
    }
    
    public VideoAdapter(List<YouTubeApiService.VideoItem> videos, OnVideoClickListener listener) {
        this.videos = videos;
        this.listener = listener;
    }
    
    public void updateVideos(List<YouTubeApiService.VideoItem> newVideos) {
        this.videos = newVideos;
        notifyDataSetChanged();
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
    public int getItemCount() {
        return videos != null ? videos.size() : 0;
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
            
            if (video.thumbnailUrl != null && !video.thumbnailUrl.isEmpty()) {
                Picasso.get()
                        .load(video.thumbnailUrl)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .into(thumbnail);
            }
            
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onVideoClick(video);
                }
            });
        }
    }
}

