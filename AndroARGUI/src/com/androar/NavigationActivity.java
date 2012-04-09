package com.androar;

import java.io.File;
import java.io.FileOutputStream;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
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
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class NavigationActivity extends Activity implements
		SurfaceHolder.Callback, OnClickListener {
	SurfaceView mSurfaceView;
	SurfaceHolder mSurfaceHolder;

	Bitmap mCameraCaptureBmp;
	Camera mCamera;
	Button mCameraCapture;
	boolean mPreviewRunning = false;
	TextView metadataPosition, metadataOrientation;

	private LocationListener locationListener;
	private LocationManager locationManager;
	private SensorManager sensorManager;
	private SensorEventListener sensorListener;

	final int CAMERA_DATA = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.navigate);
		initialize();
	}

	public void initialize() {
		mSurfaceView = (SurfaceView) findViewById(R.id.surfaceCamera);
		mSurfaceHolder = mSurfaceView.getHolder();
		mSurfaceHolder.addCallback(this);
		mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		mCameraCapture = (Button) findViewById(R.id.bNavigationCapturePhoto);
		mCameraCapture.setOnClickListener(this);

		metadataPosition = (TextView) findViewById(R.id.tvMetadataPosition);
		metadataOrientation = (TextView) findViewById(R.id.tvMetadataOrientation);
		// get orientation details
		getOrientation();
	}

	@Override
	public void onResume() {
		super.onResume();

		if (mCamera == null)
			mCamera = Camera.open();
	}

	@Override
	public void onPause() {
		if (mPreviewRunning)
			mCamera.stopPreview();

		mCamera.release();
		mCamera = null;
		mPreviewRunning = false;

		super.onPause();
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.bNavigationCapturePhoto)
			if (mPreviewRunning) {
				mCamera.takePicture(null, null, mPictureCallback);
				mPreviewRunning = false;
			}
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

	private Camera.Size getSmallestPictureSize(Camera.Parameters parameters) {
		Camera.Size result = null;

		for (Camera.Size size : parameters.getSupportedPictureSizes()) {
			if (result == null) {
				result = size;
			} else {
				int resultArea = result.width * result.height;
				int newArea = size.width * size.height;

				if (newArea < resultArea) {
					result = size;
				}
			}
		}
		return (result);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		Camera.Parameters parameters = mCamera.getParameters();
		Camera.Size size = getBestPreviewSize(width, height, parameters);
		Camera.Size pictureSize = getSmallestPictureSize(parameters);

		if (size != null && pictureSize != null) {
			parameters.setPreviewSize(size.width, size.height);
			parameters.setPictureSize(pictureSize.width, pictureSize.height);
			parameters.setPictureFormat(ImageFormat.JPEG);

			mCamera.setParameters(parameters);
			mCamera.startPreview();
			mPreviewRunning = true;
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		try {
			mCamera.setPreviewDisplay(mSurfaceHolder);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// Remove the location listener, conserve battery
		try {
			locationManager.removeUpdates(locationListener);
			sensorManager.unregisterListener(sensorListener);
		} catch (IllegalArgumentException e) {
		}
	}

	Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {

		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			new SavePhotoTask().execute(data);

			// Remove the location listener, conserve battery
			try {
				locationManager.removeUpdates(locationListener);
				sensorManager.unregisterListener(sensorListener);
			} catch (IllegalArgumentException e) {
			}

			Intent i = new Intent(NavigationActivity.this,
					MoveSelectionActivity.class);
			i.putExtra("data", data);

			camera.stopPreview();
			mPreviewRunning = false;
			startActivity(i);
		}
	};

	class SavePhotoTask extends AsyncTask<byte[], String, String> {
		@Override
		protected String doInBackground(byte[]... jpeg) {
			File photo = new File(Environment.getExternalStorageDirectory(),
					"photo.jpg");
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

	void getOrientation() {
		String position, orientation;
		
		// Acquire a reference to the system Location Manager
		locationManager = (LocationManager) this
				.getSystemService(Context.LOCATION_SERVICE);

		// Define a listener that responds to location updates
		locationListener = new LocationListener() {
			public void onLocationChanged(Location location) {
				// Called when a new location is found by the network location
				// provider.
				makeUseOfNewLocation(location);
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

		System.out.println("sensorListener = ...");

		sensorListener = new SensorEventListener() {

			@Override
			public void onSensorChanged(SensorEvent event) {
				String or = " " + "azimute:" + event.values[0];
				metadataOrientation.setText(or);
			}

			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {}
		};
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		sensorManager.registerListener(sensorListener, sensorManager
				.getDefaultSensor(Sensor.TYPE_ORIENTATION),
				SensorManager.SENSOR_DELAY_GAME);
	}

	void makeUseOfNewLocation(Location location) {
		String pos = "latitude: " + location.getLatitude()
				+ ", longitude: " + location.getLongitude() + ", height: "
				+ location.getAltitude();
		metadataPosition.setText(pos);
	}
}
