package com.androar;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
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
	private static File pictureDir;
	private float latitude = 0, longitude = 0, azimuth = 0;
	
	private LocationListener locationListener;
	private LocationManager locationManager;
	private SensorManager sensorManager;
	private SensorEventListener sensorListener;

	private static final int CROP_FROM_CAMERA = 2;

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
		inPreview = true;
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
		getOrientation();
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
		try {
			locationManager.removeUpdates(locationListener);
			sensorManager.unregisterListener(sensorListener);
		} catch (IllegalArgumentException e) {
		}
	}

	/* Menu inflate. */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.preview, menu);
		return true;
	}
	
	void getOrientation() {
		String position, orientation;

		// Acquire a reference to the system Location Manager
		locationManager = (LocationManager) this
				.getSystemService(Context.LOCATION_SERVICE);

		// Define a listener that responds to location updates
		locationListener = new LocationListener() {
			public void onLocationChanged(Location location) {
				latitude = (float) location.getLatitude();
				longitude = (float) location.getLongitude();
			}

			public void onStatusChanged(String provider, int status,
					Bundle extras) {
			}

			public void onProviderEnabled(String provider) {
			}

			public void onProviderDisabled(String provider) {
			}
		};

		// Register the listener with the Location Manager to receive location
		// updates

		locationManager.requestLocationUpdates(
				LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,
				0, locationListener);

		sensorListener = new SensorEventListener() {

			@Override
			public void onSensorChanged(SensorEvent event) {
				azimuth = event.values[0];
			}

			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
			}
		};
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		sensorManager.registerListener(sensorListener, sensorManager
				.getDefaultSensor(Sensor.TYPE_ORIENTATION),
				SensorManager.SENSOR_DELAY_GAME);
	}

	/* Add action when menu buttons pressed. */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menuCapture:
			if (inPreview) {
				camera.takePicture(null, null, mPictureCallback);
				inPreview = false;
			}
		}
		return true;
	}

	Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {

		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			new SavePhotoTask("photo.jpg").execute(data);

			Intent i = new Intent(CameraPreview.this, CropOptionActivity.class);
			camera.stopPreview();
			inPreview = false;
			startActivityForResult(i, CROP_FROM_CAMERA);
		}
	};

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// if (resultCode != RESULT_OK) return;

		switch (requestCode) {
			case CROP_FROM_CAMERA:
				/* Crop image is at /sdcard/Android/androAR/photoCropped.jpg. */
				String croppedPath = pictureDir.getPath() + "/photoCropped.jpg";
				File f = new File(croppedPath);
				if (f.exists()) {
					System.out.println("Poza a fost croppuita");
	
					try {
						// Create a new activity with a place to label the image to send to
						// server.
						Bitmap picture = BitmapFactory.decodeFile(croppedPath);
						Intent i = new Intent(this, LabelCroppedImage.class);
						i.putExtra("data", picture);
						startActivity(i);
	//					send_to_server();
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else
					System.out.println("Poza croppuita nu exista");
		}
	}

	class SavePhotoTask extends AsyncTask<byte[], String, String> {
		String photoName;
		
		public SavePhotoTask(String photoName) {
			this.photoName = photoName;
		}
		
		@Override
		protected String doInBackground(byte[]... jpeg) {
			pictureDir = new File(Environment.getExternalStorageDirectory()
					.getPath() + "/Android/data/com.androar");
			if (!pictureDir.isDirectory())
				pictureDir.mkdirs();
			File photo = new File(pictureDir, photoName);
			if (photo.exists())
				photo.delete();

			try {
				FileOutputStream fos = new FileOutputStream(photo.getPath());

				fos.write(jpeg[0]);
				fos.close();
			} catch (java.io.IOException e) {
				e.printStackTrace();
			}
			return (null);
		}
	}
	
	public static String getPictureDir() {
		return pictureDir.getPath();
	}
}