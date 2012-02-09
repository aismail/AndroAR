package com.androar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class MoveSelection extends SurfaceView implements SurfaceHolder.Callback {
	private SurfaceHolder mSurfaceHolder;
	private DrawThread mThread = null;
	private Bitmap bitmap, background;
	private float x = 0, y = 0;
	private boolean moving = false;
	
	/**
	 * Subclass of SurfaceView. Moves a bitmap on the SurfaceView by
	 * permanently redrawing it, using threads.
	 */
	public MoveSelection(Context context) {
		super(context);
		init();
	}
	
	/**
	 * Subclass of SurfaceView. Moves a bitmap on the SurfaceView by
	 * permanently redrawing it, using threads.
	 */
	public MoveSelection(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	/**
	 * Subclass of SurfaceView. Moves a bitmap on the SurfaceView by
	 * permanently redrawing it, using threads.
	 */
	public MoveSelection(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}
	
	public void init() {
		mSurfaceHolder = getHolder();
		mSurfaceHolder.addCallback(this);
		bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
		background = BitmapFactory.decodeResource(getResources(), R.drawable.street);
		mThread = new DrawThread(mSurfaceHolder, this);
		setFocusable(true);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		Rect oldR = new Rect(0, 0, background.getWidth(), background.getHeight());
		Rect newR = new Rect(0, 0, this.getWidth(), this.getHeight());
		canvas.drawBitmap(background, oldR, newR, null);
		canvas.drawBitmap(bitmap, x - (bitmap.getWidth() / 2), y - (bitmap.getHeight() / 2), null);
	}

	/*
	 * Move the bitmap only if the first click was done on it, and then moved.
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		float off_x = event.getX();
		float off_y = event.getY();

		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		switch (event.getAction()) {
		case MotionEvent.ACTION_UP:
			if (moving) {
				x = off_x;
				y = off_y;
			}
			moving = false;
			break;
		case MotionEvent.ACTION_DOWN:
			if ((Math.abs(off_x - x) <= bitmap.getWidth() / 2) &&
				(Math.abs(off_y - y) <= bitmap.getHeight() / 2))
				moving = true;
		case MotionEvent.ACTION_MOVE:
			if (moving) {
				x = off_x;
				y = off_y;
			}
			break;
		}
		return true;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		x = holder.getSurfaceFrame().exactCenterX();
		y = holder.getSurfaceFrame().exactCenterY();
		mThread.setRunning(true);
		mThread.start();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		boolean retry = true;
		mThread.setRunning(false);
		while (retry) {
			try {
				mThread.join();
				retry = false;
			} catch (InterruptedException e) {}
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {}

	/**
	 * Thread for permanently redrawing the SurfaceView.
	 */
	class DrawThread extends Thread {
		private SurfaceHolder surfaceHolder;
		private MoveSelection panel;
		private boolean run;

		public DrawThread(SurfaceHolder surfaceHolder, MoveSelection panel) {
			this.surfaceHolder = surfaceHolder;
			this.panel = panel;
		}

		public void setRunning(boolean run) {
			this.run = run;
		}

		@Override
		public void run() {
			Canvas c;
			while (run) {
				c = null;
				try {
					c = surfaceHolder.lockCanvas();
					synchronized (surfaceHolder) {
						panel.onDraw(c);
					}
				} finally {
					if (c != null)
						surfaceHolder.unlockCanvasAndPost(c);
				}
			}
		}
	}
}