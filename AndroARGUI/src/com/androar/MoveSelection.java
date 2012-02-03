package com.androar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;

public class MoveSelection extends SurfaceView implements Runnable, OnTouchListener, SurfaceHolder.Callback {
	private SurfaceHolder holder;
	private Thread thread = null;
	private Bitmap bitmap;
	private float x = 0, y = 0;
	private boolean paused = true, moving = false;

	public MoveSelection(Context context) {
		super(context);
		holder = getHolder();
		holder.addCallback(this);
		bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
		
		System.out.println(bitmap.getWidth() + " " + bitmap.getHeight());
		resizeBitmap(100, 100);
		System.out.println(bitmap.getWidth() + " " + bitmap.getHeight());
	}
	
	/* Resize a bitmap to the newWidth 
	 * and newHeight using a Matrix
	 */
	public void resizeBitmap(int newWidth, int newHeight) {
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		
		float scaleW = ((float) newWidth) / width;
		float scaleH = ((float) newHeight) / height;
		
		Matrix matrix = new Matrix();
		matrix.postScale(scaleW, scaleH);
		// resize the bitmap now
		bitmap = Bitmap.createBitmap(bitmap, (int) x, (int) y, width, height, matrix, true);
	}
	
	@Override
	public void run() {
		while(!paused) {
			if (!holder.getSurface().isValid())
				continue;
			
			Canvas c = holder.lockCanvas();
			c.drawARGB(255, 50, 50, 100);
			c.drawBitmap(bitmap, x - bitmap.getWidth()/2, y - bitmap.getHeight()/2, null);
			holder.unlockCanvasAndPost(c);
		}
	}
		
	public void pause() {
		paused = true;
		while(true) {
			try {
				thread.join();
			} catch(InterruptedException e) {
				e.printStackTrace();
			}
			break;
		}
		thread = null;
	}
		
	public void resume() {
		paused = false;
		thread = new Thread(this);
		thread.start();
	}

	/* Move the bitmap only if the first click was done
	 * on it, and then moved.
	 */
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		float off_x = event.getX();
		float off_y = event.getY();
		
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		switch(event.getAction()) {
		case MotionEvent.ACTION_UP:
			if (moving) {
				x = off_x;
				y = off_y;
			}
			moving = false;
			break;
		case MotionEvent.ACTION_DOWN:
			if ( ( Math.abs(off_x - x) <= bitmap.getWidth() / 2 ) &&
			   ( Math.abs(off_y - y) <= bitmap.getHeight() / 2 )  )
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
	}
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {}
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}
}
