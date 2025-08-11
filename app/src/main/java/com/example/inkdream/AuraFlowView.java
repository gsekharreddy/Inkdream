// file: app/src/main/java/com/example/auraflow/AuraFlowView.java
// Make sure the package name matches your project's package name.
package com.example.inkdream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.media.audiofx.Visualizer;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * This is the core of the application. It's a custom SurfaceView that handles:
 * 1. The main animation loop in a separate thread.
 * 2. Drawing all the particles.
 * 3. Handling multi-touch input from the user.
 */
public class AuraFlowView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

	private SurfaceHolder surfaceHolder;
	private Thread drawingThread;
	private volatile boolean isRunning = false;

	private final List<Particle> particles = Collections.synchronizedList(new ArrayList<>());
	private final Map<Integer, PointF> lastTouchPoints = new HashMap<>();

	private int hue = 0;

	private Bitmap offscreenBitmap;
	private Canvas offscreenCanvas;
	private Paint paintForBitmap;

	private volatile boolean isRandomMode = false;
	private float randomSpeedMultiplier = 1.0f;
	private final Random random = new Random();

	// --- NEW: Fields for audio visualization ---
	private Visualizer visualizer;
	private byte[] fftBytes;
	private float audioMagnitude = 0f;


	public AuraFlowView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init() {
		surfaceHolder = getHolder();
		surfaceHolder.addCallback(this);
		surfaceHolder.setFormat(PixelFormat.TRANSLUCENT);

		paintForBitmap = new Paint();
		paintForBitmap.setAntiAlias(true);
	}

	// --- NEW: Method to set up the Visualizer ---
	private void setupVisualizer() {
		try {
			visualizer = new Visualizer(0); // 0 = system-wide audio output
			visualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[0]); // Use the smallest capture size

			visualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
				@Override
				public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
					// Not used for this effect
				}

				@Override
				public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
					// This gives us the frequency data of the audio
					fftBytes = fft;
				}
			}, Visualizer.getMaxCaptureRate() / 2, false, true); // Waveform false, FFT true

			visualizer.setEnabled(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		if (offscreenBitmap == null || offscreenBitmap.getWidth() != width || offscreenBitmap.getHeight() != height) {
			if (offscreenBitmap != null) {
				offscreenBitmap.recycle();
			}
			offscreenBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			offscreenCanvas = new Canvas(offscreenBitmap);
		}
	}


	@Override
	public void run() {
		while (isRunning) {
			Canvas canvas = null;
			try {
				canvas = surfaceHolder.lockCanvas();
				if (canvas != null) {
					synchronized (surfaceHolder) {
						updateAndDraw();
						canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
						canvas.drawBitmap(offscreenBitmap, 0, 0, paintForBitmap);
					}
				}
			} finally {
				if (canvas != null) {
					surfaceHolder.unlockCanvasAndPost(canvas);
				}
			}
		}
	}

	private void updateAndDraw() {
		if (offscreenCanvas == null) return;

		// --- NEW: Update audio magnitude from FFT data ---
		if (fftBytes != null) {
			float currentMagnitude = 0;
			for (int i = 2; i < fftBytes.length; i += 2) { // Skip the DC component
				// Calculate magnitude of the complex number
				currentMagnitude += Math.hypot(fftBytes[i], fftBytes[i + 1]);
			}
			// Smoothly update the magnitude value
			audioMagnitude = (audioMagnitude * 0.8f) + ((currentMagnitude / fftBytes.length) * 0.2f);
		}


		offscreenCanvas.drawColor(Color.argb(30, 0, 0, 0));

		if (isRandomMode) {
			if (random.nextInt(10) < 3) {
				float randomX = random.nextFloat() * getWidth();
				float randomY = random.nextFloat() * getHeight();
				int burst = 2;
				for (int i = 0; i < burst; i++) {
					particles.add(new Particle(randomX, randomY, hue, randomSpeedMultiplier, audioMagnitude));
				}
			}
		}

		synchronized (particles) {
			for (int i = particles.size() - 1; i >= 0; i--) {
				Particle p = particles.get(i);
				if (p.update()) {
					p.draw(offscreenCanvas);
				} else {
					particles.remove(i);
				}
			}
		}

		hue = (hue + 2) % 360;
	}

	public void setRandomMode(boolean isRandom) {
		this.isRandomMode = isRandom;
	}

	public void setRandomSpeedMultiplier(int progress) {
		this.randomSpeedMultiplier = progress / 100.0f;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getActionMasked();
		int pointerIndex = event.getActionIndex();
		int pointerId = event.getPointerId(pointerIndex);

		switch (action) {
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_POINTER_DOWN:
				// --- MODIFIED: Pass audio magnitude to touch particles ---
				particles.add(new Particle(event.getX(pointerIndex), event.getY(pointerIndex), hue, 1.0f, audioMagnitude));
				lastTouchPoints.put(pointerId, new PointF(event.getX(pointerIndex), event.getY(pointerIndex)));
				break;

			case MotionEvent.ACTION_MOVE:
				for (int i = 0; i < event.getPointerCount(); i++) {
					int id = event.getPointerId(i);
					PointF currentPoint = new PointF(event.getX(i), event.getY(i));
					PointF lastPoint = lastTouchPoints.get(id);

					if (lastPoint != null) {
						float dx = currentPoint.x - lastPoint.x;
						float dy = currentPoint.y - lastPoint.y;
						float distance = (float) Math.sqrt(dx * dx + dy * dy);

						float particleSpacing = 15;
						for (float j = 0; j < distance; j += particleSpacing) {
							float interpolatedX = lastPoint.x + (dx * (j / distance));
							float interpolatedY = lastPoint.y + (dy * (j / distance));
							// --- MODIFIED: Pass audio magnitude to touch particles ---
							particles.add(new Particle(interpolatedX, interpolatedY, hue, 1.0f, audioMagnitude));
						}
					}
					lastTouchPoints.put(id, currentPoint);
				}
				break;

			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_POINTER_UP:
			case MotionEvent.ACTION_CANCEL:
				lastTouchPoints.remove(pointerId);
				break;
		}
		return true;
	}


	@Override
	public void surfaceCreated(SurfaceHolder holder) { }

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		pause();
		if (offscreenBitmap != null) {
			offscreenBitmap.recycle();
			offscreenBitmap = null;
		}
	}

	@Override
	public void resume() {
		isRunning = true;
		drawingThread = new Thread(this);
		drawingThread.start();
		// --- NEW: Start the visualizer ---
		setupVisualizer();
	}

	@Override
	public void pause() {
		isRunning = false;
		try {
			if (drawingThread != null) {
				drawingThread.join();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		// --- NEW: Release the visualizer ---
		if (visualizer != null) {
			visualizer.release();
			visualizer = null;
		}
	}
}
