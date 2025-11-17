package com.yausername.youtubedl_android_example;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.yausername.youtubedl_android.YoutubeDL;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btnUpdate;
    private Button btnLogin;
    private Button btnSubscriptions;
    private Button btnPlaylists;
    private Button btnRecommendations;
    private TextView tvLoginStatus;
    private ProgressBar progressBar;

    private boolean updating = false;
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private GoogleAuthHelper authHelper;
    private static final int REQUEST_CODE_LOGIN = 1001;

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        authHelper = new GoogleAuthHelper(this);
        initViews();
        initListeners();
        updateLoginStatus();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateLoginStatus();
    }

    @Override
    protected void onDestroy() {
        compositeDisposable.dispose();
        super.onDestroy();
    }

    private void initListeners() {
        btnUpdate.setOnClickListener(this);
        btnLogin.setOnClickListener(this);
        btnSubscriptions.setOnClickListener(this);
        btnPlaylists.setOnClickListener(this);
        btnRecommendations.setOnClickListener(this);
    }

    private void initViews() {
        btnUpdate = findViewById(R.id.btn_update);
        btnLogin = findViewById(R.id.btn_login);
        btnSubscriptions = findViewById(R.id.btn_subscriptions);
        btnPlaylists = findViewById(R.id.btn_playlists);
        btnRecommendations = findViewById(R.id.btn_recommendations);
        tvLoginStatus = findViewById(R.id.tv_login_status);
        progressBar = findViewById(R.id.progress_bar);
    }
    
    private void updateLoginStatus() {
        boolean isLoggedIn = authHelper.isLoggedIn();
        if (isLoggedIn) {
            tvLoginStatus.setText("Logged in");
            btnLogin.setText("Logout");
            btnSubscriptions.setEnabled(true);
            btnPlaylists.setEnabled(true);
            btnRecommendations.setEnabled(true);
        } else {
            tvLoginStatus.setText("Not logged in");
            btnLogin.setText("Login with Google");
            btnSubscriptions.setEnabled(false);
            btnPlaylists.setEnabled(false);
            btnRecommendations.setEnabled(false);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.btn_login) {
            if (authHelper.isLoggedIn()) {
                authHelper.clearTokens();
                updateLoginStatus();
                Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
            } else {
                Intent i = new Intent(MainActivity.this, LoginActivity.class);
                startActivityForResult(i, REQUEST_CODE_LOGIN);
            }
        } else if (id == R.id.btn_subscriptions) {
            Intent i = new Intent(MainActivity.this, SubscriptionsActivity.class);
            startActivity(i);
        } else if (id == R.id.btn_playlists) {
            Intent i = new Intent(MainActivity.this, PlaylistListActivity.class);
            startActivity(i);
        } else if (id == R.id.btn_recommendations) {
            Intent i = new Intent(MainActivity.this, RecommendationsActivity.class);
            startActivity(i);
        } else if (id == R.id.btn_update) {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("Update Channel")
                    .setItems(new String[]{"Stable Releases", "Nightly Releases", "Master Releases"},
                            (dialogInterface, which) -> {
                                if (which == 0)
                                    updateYoutubeDL(YoutubeDL.UpdateChannel._STABLE);
                                else if (which == 1)
                                    updateYoutubeDL(YoutubeDL.UpdateChannel._NIGHTLY);
                                else
                                    updateYoutubeDL(YoutubeDL.UpdateChannel._MASTER);
                            })
                    .create();
            dialog.show();
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_LOGIN && resultCode == RESULT_OK) {
            updateLoginStatus();
        }
    }

    private void updateYoutubeDL(YoutubeDL.UpdateChannel updateChannel) {
        if (updating) {
            Toast.makeText(MainActivity.this, "Update is already in progress!", Toast.LENGTH_LONG).show();
            return;
        }

        updating = true;
        progressBar.setVisibility(View.VISIBLE);
        Disposable disposable = Observable.fromCallable(() -> YoutubeDL.getInstance().updateYoutubeDL(this, updateChannel))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(status -> {
                    progressBar.setVisibility(View.GONE);
                    switch (status) {
                        case DONE:
                            Toast.makeText(MainActivity.this, "Update successful " + YoutubeDL.getInstance().versionName(this), Toast.LENGTH_LONG).show();
                            break;
                        case ALREADY_UP_TO_DATE:
                            Toast.makeText(MainActivity.this, "Already up to date " + YoutubeDL.getInstance().versionName(this), Toast.LENGTH_LONG).show();
                            break;
                        default:
                            Toast.makeText(MainActivity.this, status.toString(), Toast.LENGTH_LONG).show();
                            break;
                    }
                    updating = false;
                }, e -> {
                    Log.e(TAG, "failed to update", e);
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "update failed", Toast.LENGTH_LONG).show();
                    updating = false;
                });
        compositeDisposable.add(disposable);
    }
}
