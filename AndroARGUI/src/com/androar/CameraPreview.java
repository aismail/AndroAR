package com.androar;

import java.util.ArrayList;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.LinearLayout;
import android.widget.Toast;

public class CameraPreview extends Activity implements SurfaceHolder.Callback {

	private SurfaceView surfaceView;
	private SurfaceHolder holder;
	private Camera camera;
	private boolean inPreview = false;
	private ArrayList<Rect> rectangles;
	private RenderRectanglesView rectanglesView;
	private LinearLayout layout;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.camera_preview);
		init();
	}

	// public void addMockRect() {
	// rectangles.add(new Rect(30, 10, 90, 90));
	// }

	public void init() {
		// We register the activity to handle the callbacks of the SurfaceView
		surfaceView = (SurfaceView) findViewById(R.id.camera_surface);
		holder = surfaceView.getHolder();
		holder.addCallback(this);
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		rectangles = new ArrayList<Rect>();
		// addMockRect();
		rectanglesView = (RenderRectanglesView) findViewById(R.id.render_rectangles);

		// Add touch event to layout
		layout = (LinearLayout) findViewById(R.id.top_panel);
		layout.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				int x = (int) event.getX();
				int y = (int) event.getY();
				rectangles.clear();
				rectangles.add(new Rect(x, y, x + 20, y + 40));
				rectanglesView.invalidate();
				return true;
			}
		});
	}

	@Override
	public void onResume() {
		super.onResume();
		camera = Camera.open();
	}

	@Override
	public void onPause() {
		if (inPreview) {
			camera.stopPreview();
		}
		camera.release();
		camera = null;
		inPreview = false;
		super.onPause();
	}

	private Camera.Size getBestPreviewSize(int width, int height,
			Camera.Parameters parameters) {
		Camera.Size result = null;

		for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
			if (size.width <= width && size.height <= height) {
				if (result == null) {
					result = size;
				} else {
					int resultArea = result.width * result.height;
					int newArea = size.width * size.height;

					if (newArea > resultArea) {
						result = size;
					}
				}
			}
		}
		return (result);
	}

	public void surfaceCreated(SurfaceHolder holder) {
		try {
			camera.setPreviewDisplay(holder);
		} catch (Throwable t) {
			Log.e("PreviewDemo-surfaceCallback",
					"Exception in setPreviewDisplay()", t);
			Toast.makeText(CameraPreview.this, t.getMessage(),
					Toast.LENGTH_LONG).show();
		}
		rectanglesView.setWillNotDraw(false);
		rectanglesView.init();
		rectanglesView.setRects(rectangles);
		rectanglesView.invalidate();
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		System.out.println("changed");
		try {
			rectanglesView.setPaintColor(Color.CYAN);
			rectangles.add(new Rect(50, 30, 70, 70));
			rectanglesView.setRects(rectangles);
			rectanglesView.invalidate();
		} catch (Throwable t) {
		}
		Camera.Parameters params = camera.getParameters();
		Camera.Size size = getBestPreviewSize(width, height, params);

		if (size != null) {
			params.setPreviewSize(width, height);
			camera.setParameters(params);
			camera.startPreview();
			inPreview = true;
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
	}

	/* Menu inflate. */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.preview, menu);
		return true;
	}

	/* Add action when menu buttons pressed. */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menuCapture:

		}
		return true;
	}
}