package com.androar;

import java.io.File;
import java.io.FileOutputStream;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore.Images.Media;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;

public class NavigationActivity extends Activity implements
		SurfaceHolder.Callback, OnClickListener {
	SurfaceView mSurfaceView;
	SurfaceHolder mSurfaceHolder;

	Bitmap mCameraCaptureBmp;
	Camera mCamera;
	Button mCameraCapture;
	boolean mPreviewRunning = false;

	final int CAMERA_DATA = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		// getWindow().setFormat(PixelFormat.TRANSLUCENT);
		// getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
		// WindowManager.LayoutParams.FLAG_FULLSCREEN);
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
		// nothing
	}

	Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {

		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			new SavePhotoTask().execute(data);

			Intent i = new Intent(NavigationActivity.this, MoveSelectionActivity.class);
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
}
