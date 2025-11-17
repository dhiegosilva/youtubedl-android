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

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {
    private List<YouTubeApiService.PlaylistInfo> playlists;
    private OnPlaylistClickListener listener;
    
    public interface OnPlaylistClickListener {
        void onPlaylistClick(YouTubeApiService.PlaylistInfo playlist);
    }
    
    public PlaylistAdapter(List<YouTubeApiService.PlaylistInfo> playlists, OnPlaylistClickListener listener) {
        this.playlists = playlists;
        this.listener = listener;
    }
    
    public void updatePlaylists(List<YouTubeApiService.PlaylistInfo> newPlaylists) {
        this.playlists = newPlaylists;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_playlist, parent, false);
        return new PlaylistViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        YouTubeApiService.PlaylistInfo playlist = playlists.get(position);
        holder.bind(playlist, listener);
    }
    
    @Override
    public int getItemCount() {
        return playlists != null ? playlists.size() : 0;
    }
    
    static class PlaylistViewHolder extends RecyclerView.ViewHolder {
        private ImageView thumbnail;
        private TextView title;
        private TextView itemCount;
        
        public PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.thumbnail);
            title = itemView.findViewById(R.id.title);
            itemCount = itemView.findViewById(R.id.item_count);
        }
        
        public void bind(YouTubeApiService.PlaylistInfo playlist, OnPlaylistClickListener listener) {
            title.setText(playlist.title);
            
            if (playlist.itemCount != null) {
                itemCount.setText(playlist.itemCount + " videos");
            } else {
                itemCount.setText("0 videos");
            }
            
            if (playlist.thumbnailUrl != null && !playlist.thumbnailUrl.isEmpty()) {
                Picasso.get()
                        .load(playlist.thumbnailUrl)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .into(thumbnail);
            }
            
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlaylistClick(playlist);
                }
            });
        }
    }
}

