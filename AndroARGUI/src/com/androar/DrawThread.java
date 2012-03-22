package com.androar;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

/**
 * Thread for permanently redrawing the SurfaceView.
 */
public class DrawThread extends Thread {
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
