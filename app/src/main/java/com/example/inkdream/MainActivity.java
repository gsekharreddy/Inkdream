// file: app/src/main/java/com/example/auraflow/MainActivity.java
// Make sure the package name matches your project's package name.
package com.example.inkdream;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Locale;

/**
 * The main screen of the app. It controls the UI elements like the timer
 * and the ad overlay, and tells the AuraFlowView when to start and stop animating.
 */
public class MainActivity extends AppCompatActivity {

	private static final long PLAY_TIME_MILLIS = 180 * 1000;
	private static final int AUDIO_PERMISSION_REQUEST_CODE = 101;

	private AuraFlowView auraFlowView;
	private TextView timerTextView;
	private FrameLayout adOverlay;
	private Button playAgainButton;
	private CountDownTimer countDownTimer;
	private CheckBox randomizeCheckbox;
	private SeekBar speedSeekBar;

	// --- NEW: Reference to the Music CheckBox ---
	private CheckBox musicCheckbox;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		getWindow().getDecorView().setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
						| View.SYSTEM_UI_FLAG_LAYOUT_STABLE
						| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
						| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
						| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
						| View.SYSTEM_UI_FLAG_FULLSCREEN);

		auraFlowView = findViewById(R.id.auraFlowView);
		timerTextView = findViewById(R.id.timerTextView);
		adOverlay = findViewById(R.id.ad_overlay);
		playAgainButton = findViewById(R.id.playAgainButton);
		randomizeCheckbox = findViewById(R.id.randomizeCheckbox);
		speedSeekBar = findViewById(R.id.speedSeekBar);

		// --- NEW: Find the Music CheckBox ---
		musicCheckbox = findViewById(R.id.musicCheckbox);

		setupListeners();
		startSession();
	}

	private void setupListeners() {
		randomizeCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
			auraFlowView.setRandomMode(isChecked);
			speedSeekBar.setVisibility(isChecked ? View.VISIBLE : View.GONE);
		});

		speedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				auraFlowView.setRandomSpeedMultiplier(progress);
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {}
		});

		// --- NEW: Logic for the Music CheckBox ---
		musicCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
			if (isChecked) {
				// User wants to enable music mode, check for permission first.
				if (checkAudioPermission()) {
					auraFlowView.setMusicMode(true);
				} else {
					// If permission not granted, uncheck the box and request permission.
					buttonView.setChecked(false);
					requestAudioPermission();
				}
			} else {
				// User wants to disable music mode.
				auraFlowView.setMusicMode(false);
			}
		});

		playAgainButton.setOnClickListener(v -> {
			adOverlay.setVisibility(View.GONE);
			startSession();
		});
	}

	private void startSession() {
		if (countDownTimer != null) {
			countDownTimer.cancel();
		}

		countDownTimer = new CountDownTimer(PLAY_TIME_MILLIS, 1000) {
			@Override
			public void onTick(long millisUntilFinished) {
				long minutes = (millisUntilFinished / 1000) / 60;
				long seconds = (millisUntilFinished / 1000) % 60;
				String timeFormatted = String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
				timerTextView.setText(timeFormatted);
			}

			@Override
			public void onFinish() {
				timerTextView.setText("0:00");
				adOverlay.setVisibility(View.VISIBLE);
				auraFlowView.pause();
			}
		}.start();

		auraFlowView.resume();
	}

	private boolean checkAudioPermission() {
		return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
	}

	private void requestAudioPermission() {
		ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, AUDIO_PERMISSION_REQUEST_CODE);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == AUDIO_PERMISSION_REQUEST_CODE) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				// Permission was granted, now we can check the box and enable the mode.
				musicCheckbox.setChecked(true);
				auraFlowView.setMusicMode(true);
			} else {
				Toast.makeText(this, "Audio permission is required for music visualization.", Toast.LENGTH_LONG).show();
			}
		}
	}


	@Override
	protected void onResume() {
		super.onResume();
		if (adOverlay.getVisibility() == View.GONE) {
			auraFlowView.resume();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		auraFlowView.pause();
	}
}
