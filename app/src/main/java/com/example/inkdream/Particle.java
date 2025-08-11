// file: app/src/main/java/com/example/auraflow/Particle.java
// Make sure the package name matches your project's package name.
package com.example.inkdream;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;

import java.util.Random;

/**
 * Represents a single, soft, glowing orb. This version is simplified for performance
 * and designed to be created in large numbers to form a continuous stream.
 */
public class Particle {
	private float x, y;
	private float baseSize; // The original size of the particle
	private float currentSize; // The size after being affected by music
	private float speedX, speedY;
	private int alpha;
	private final int decay;
	private final int hue;
	private final Paint paint;
	private final Random random = new Random();

	// --- MODIFIED: Constructor no longer takes audioMagnitude ---
	public Particle(float x, float y, int initialHue, float speedMultiplier) {
		this.x = x;
		this.y = y;
		this.hue = initialHue;

		this.baseSize = random.nextFloat() * 40 + 30;
		this.currentSize = baseSize; // Initially, current size is the same as base size

		this.speedX = (random.nextFloat() * 1.0f - 0.5f) * speedMultiplier;
		this.speedY = (random.nextFloat() * 1.0f - 0.5f) * speedMultiplier;
		this.alpha = 150 + random.nextInt(50);
		this.decay = 8 + random.nextInt(6);

		this.paint = new Paint();
		this.paint.setAntiAlias(true);
		this.paint.setStyle(Paint.Style.FILL);
	}

	// --- MODIFIED: Update method now takes audioMagnitude ---
	public boolean update(float audioMagnitude) {
		// Update position
		this.x += this.speedX;
		this.y += this.speedY;

		// Add a slight wobble for a more organic path
		this.speedX += random.nextFloat() * 0.2f - 0.1f;
		this.speedY += random.nextFloat() * 0.2f - 0.1f;

		// --- NEW: Update the particle's size based on the music ---
		float sizeBonus = audioMagnitude * 5;
		this.currentSize = baseSize + sizeBonus;

		// Fade out the particle
		this.alpha -= this.decay;
		return this.alpha > 0;
	}

	/**
	 * Draws the particle as a single, soft orb using a RadialGradient.
	 * @param canvas The canvas to draw on.
	 */
	public void draw(Canvas canvas) {
		float[] hsl = {hue, 1.0f, 0.7f};
		int centerColor = Color.HSVToColor(alpha, hsl);
		int edgeColor = Color.TRANSPARENT;

		RadialGradient gradient = new RadialGradient(
				x, y, currentSize, // Use the current (music-affected) size
				centerColor, edgeColor,
				Shader.TileMode.CLAMP
		);

		paint.setShader(gradient);
		canvas.drawCircle(x, y, currentSize, paint);
	}
}
