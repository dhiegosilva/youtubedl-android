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

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {
    private List<YouTubeApiService.PlaylistInfo> playlists;
    private OnPlaylistClickListener listener;
    private static final ExecutorService diffExecutor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final int DIFF_UTIL_THRESHOLD = 50; // Use DiffUtil only for lists > 50 items
    
    public interface OnPlaylistClickListener {
        void onPlaylistClick(YouTubeApiService.PlaylistInfo playlist);
    }
    
    public PlaylistAdapter(List<YouTubeApiService.PlaylistInfo> playlists, OnPlaylistClickListener listener) {
        this.playlists = playlists != null ? new ArrayList<>(playlists) : new ArrayList<>();
        this.listener = listener;
    }
    
    public void updatePlaylists(List<YouTubeApiService.PlaylistInfo> newPlaylists) {
        if (newPlaylists == null) {
            newPlaylists = new ArrayList<>();
        }
        
        final List<YouTubeApiService.PlaylistInfo> oldPlaylists = this.playlists;
        final List<YouTubeApiService.PlaylistInfo> finalNewPlaylists = new ArrayList<>(newPlaylists);
        
        // For small lists, use simple update to avoid overhead
        if (oldPlaylists.size() < DIFF_UTIL_THRESHOLD && finalNewPlaylists.size() < DIFF_UTIL_THRESHOLD) {
            this.playlists = finalNewPlaylists;
            notifyDataSetChanged();
            return;
        }
        
        // Calculate DiffUtil on background thread to avoid blocking main thread
        diffExecutor.execute(() -> {
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new PlaylistDiffCallback(oldPlaylists, finalNewPlaylists), true);
            mainHandler.post(() -> {
                this.playlists = finalNewPlaylists;
                diffResult.dispatchUpdatesTo(this);
            });
        });
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
    public void onViewRecycled(@NonNull PlaylistViewHolder holder) {
        super.onViewRecycled(holder);
        // Cancel any pending image loads when view is recycled
        Picasso.get().cancelRequest(holder.thumbnail);
    }
    
    @Override
    public int getItemCount() {
        return playlists.size();
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
            
            // Cancel any previous image load
            Picasso.get().cancelRequest(thumbnail);
            
            if (playlist.thumbnailUrl != null && !playlist.thumbnailUrl.isEmpty()) {
                // Resize image to save memory
                Picasso.get()
                        .load(playlist.thumbnailUrl)
                        .resize(480, 360) // Optimized size for memory efficiency
                        .centerCrop()
                        .onlyScaleDown() // Only scale down, never up (saves CPU)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .into(thumbnail);
            } else {
                thumbnail.setImageResource(android.R.drawable.ic_menu_gallery);
            }
            
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlaylistClick(playlist);
                }
            });
        }
    }
    
    private static class PlaylistDiffCallback extends DiffUtil.Callback {
        private final List<YouTubeApiService.PlaylistInfo> oldList;
        private final List<YouTubeApiService.PlaylistInfo> newList;
        
        PlaylistDiffCallback(List<YouTubeApiService.PlaylistInfo> oldList, List<YouTubeApiService.PlaylistInfo> newList) {
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
            return oldList.get(oldItemPosition).playlistId != null &&
                   oldList.get(oldItemPosition).playlistId.equals(newList.get(newItemPosition).playlistId);
        }
        
        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            // Simplified comparison - only check playlistId for performance
            YouTubeApiService.PlaylistInfo oldItem = oldList.get(oldItemPosition);
            YouTubeApiService.PlaylistInfo newItem = newList.get(newItemPosition);
            return oldItem.playlistId != null && oldItem.playlistId.equals(newItem.playlistId);
        }
        
        @Override
        public Object getChangePayload(int oldItemPosition, int newItemPosition) {
            // Return null to indicate full update if needed
            return null;
        }
    }
}

