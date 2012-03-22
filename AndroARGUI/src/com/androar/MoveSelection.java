package com.androar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceView;

public class MoveSelection extends SurfaceView {

	private Bitmap bitmap, background;
	private float x = 0, y = 0;
	float saved_dx = 0, saved_dy = 0;
	private float lastDistance;
	
	// selection modes
	private final int NONE = 0;
	private final int DRAG = 1;
	private final int PINCH = 2;
	private int MODE = NONE;

	private final float DEFAULT_EPSILON = (float) 0.5;

	private class ImageParams {
		// TODO: encapsulation stuff here
		float epsilon = DEFAULT_EPSILON; // error of approximation

		public float euclidianDist(float x1, float y1, float x2, float y2) {
			float dx = x1 - x2;
			float dy = y1 - y2;
			return (float) Math.sqrt(dx * dx + dy * dy);
		}
	}
	
	public void setBackground(Bitmap bitmap) {
		background = bitmap;
	}
	
	public void setSelection(Bitmap bitmap) {
		this.bitmap = bitmap;
	}
	
	public void setCoords(float x, float y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * Subclass of SurfaceView. Moves a bitmap on the SurfaceView by
	 * permanently redrawing it, using threads.
	 */
	public MoveSelection(Context context) {
		super(context);
	}

	/**
	 * Subclass of SurfaceView. Moves a bitmap on the SurfaceView by
	 * permanently redrawing it, using threads.
	 */
	public MoveSelection(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	/**
	 * Subclass of SurfaceView. Moves a bitmap on the SurfaceView by
	 * permanently redrawing it, using threads.
	 */
	public MoveSelection(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		Rect oldR = new Rect(0, 0, background.getWidth(), background.getHeight());
		Rect newR = new Rect(0, 0, this.getWidth(), this.getHeight());
		canvas.drawBitmap(background, oldR, newR, null);
		canvas.drawBitmap(bitmap, x - (bitmap.getWidth() / 2), y - (bitmap.getHeight() / 2), null);
	}
	
	/* Resize a bitmap to the newWidth 
	 * and newHeight using a Matrix
	 */
	public void resizeBitmap(float deltaX, float deltaY) {
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		
		float scaleW = (width + deltaX) / width;
		float scaleH = (height + deltaY) / height;
		Matrix matrix = new Matrix();
		matrix.postScale(scaleW, scaleH);
		// resize the bitmap now
		bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height,
				matrix, true);
	}

	/*
	 * Move the bitmap only if the first click was done on it, and then moved.
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		float current_x = event.getX();
		float current_y = event.getY();
		ImageParams image = new ImageParams();

		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_POINTER_UP:
			if (MODE == PINCH) {
				MODE = NONE;
			}
			break;
		case MotionEvent.ACTION_UP:
			if (MODE == DRAG) {
				x = current_x - saved_dx;
				y = current_y - saved_dy;
			}
			MODE = NONE;
			break;
		case MotionEvent.ACTION_DOWN:
			if ((Math.abs(current_x - x) <= bitmap.getWidth() / 2)
					&& (Math.abs(current_y - y) <= bitmap.getHeight() / 2)) {
				MODE = DRAG;
				saved_dx = current_x - x;
				saved_dy = current_y - y;
			}
			break;
		case MotionEvent.ACTION_MOVE:
			if (MODE == DRAG) {
				x = current_x - saved_dx;
				y = current_y - saved_dy;
			} else if (MODE == PINCH) {
				float newDistance = image.euclidianDist(event.getX(0), event
						.getY(0), event.getX(1), event.getY(1));
				if (newDistance > 10f) {
					float delta = newDistance - lastDistance;
					resizeBitmap(delta, delta);
					lastDistance = newDistance;
				}
			}
			break;
		case MotionEvent.ACTION_POINTER_DOWN:
			lastDistance = image.euclidianDist(event.getX(0), event.getY(0), event
					.getX(1), event.getY(1));
			if (lastDistance > 10f)
				MODE = PINCH;
			break;
		}
		return true;
	}
}