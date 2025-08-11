// file: app/src/main/java/com/example/auraflow/MainActivity.java
// Make sure the package name matches your project's package name.
package com.example.inkdream;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

/**
 * The main screen of the app. It controls the UI elements like the timer
 * and the ad overlay, and tells the AuraFlowView when to start and stop animating.
 */
public class MainActivity extends AppCompatActivity {

	private static final long PLAY_TIME_MILLIS = 180 * 1000; // 3 minutes

	private AuraFlowView auraFlowView;
	private TextView timerTextView;
	private FrameLayout adOverlay;
	private Button playAgainButton;
	private CountDownTimer countDownTimer;
	private CheckBox randomizeCheckbox;

	// --- NEW: Reference to the SeekBar ---
	private SeekBar speedSeekBar;

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

		// --- NEW: Find the SeekBar ---
		speedSeekBar = findViewById(R.id.speedSeekBar);

		// --- NEW: Logic for showing/hiding the SeekBar ---
		randomizeCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				auraFlowView.setRandomMode(isChecked);
				if (isChecked) {
					speedSeekBar.setVisibility(View.VISIBLE);
				} else {
					speedSeekBar.setVisibility(View.GONE);
				}
			}
		});

		// --- NEW: Listener for when the SeekBar value changes ---
		speedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				// The progress value is 0-200. We'll convert this to a multiplier in AuraFlowView.
				auraFlowView.setRandomSpeedMultiplier(progress);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// Not needed
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// Not needed
			}
		});


		playAgainButton.setOnClickListener(v -> {
			adOverlay.setVisibility(View.GONE);
			startSession();
		});

		startSession();
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
