package com.yausername.youtubedl_android_example;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

import com.squareup.picasso.LruCache;
import com.squareup.picasso.Picasso;
import com.yausername.aria2c.Aria2c;
import com.yausername.ffmpeg.FFmpeg;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.exceptions.UndeliverableException;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

public class App extends Application {

    private static final String TAG = "App";

    @Override
    public void onCreate() {
        super.onCreate();

        configurePicasso();
        configureRxJavaErrorHandler();
        Completable.fromAction(this::initLibraries).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new DisposableCompletableObserver() {
            @Override
            public void onComplete() {
                // it worked
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "failed to initialize youtubedl-android", e);
                Toast.makeText(getApplicationContext(), "initialization failed: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void configurePicasso() {
        // Configure Picasso with optimized memory cache (15MB) to reduce memory usage
        // Default is 15% of available memory, which can be too much on low-end devices
        long maxMemory = Runtime.getRuntime().maxMemory();
        long cacheSize = Math.min(15 * 1024 * 1024, maxMemory / 8); // Max 15MB or 1/8 of available memory
        
        Picasso picasso = new Picasso.Builder(this)
                .memoryCache(new LruCache((int) cacheSize))
                .indicatorsEnabled(false) // Disable debug indicators in production
                .loggingEnabled(false) // Disable logging to save CPU
                .build();
        Picasso.setSingletonInstance(picasso);
    }

    private void configureRxJavaErrorHandler() {
        RxJavaPlugins.setErrorHandler(e -> {

            if (e instanceof UndeliverableException) {
                // As UndeliverableException is a wrapper, get the cause of it to get the "real" exception
                e = e.getCause();
            }

            if (e instanceof InterruptedException) {
                // fine, some blocking code was interrupted by a dispose call
                return;
            }

            Log.e(TAG, "Undeliverable exception received, not sure what to do", e);
        });
    }

    private void initLibraries() throws YoutubeDLException {
        YoutubeDL.getInstance().init(this);
        FFmpeg.getInstance().init(this);
        Aria2c.getInstance().init(this);
    }
}
