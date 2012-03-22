package com.androar;

import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;

public class NavigationActivity extends Activity implements
		SurfaceHolder.Callback, OnClickListener {
	SurfaceView mSurfaceView;
	SurfaceHolder mSurfaceHolder;
	
	Bitmap mCameraCaptureBmp;
	Camera mCamera;
	boolean mPreviewRunning = true;
	
	final int CAMERA_DATA = 0;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFormat(PixelFormat.TRANSLUCENT);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.navigate);

		mSurfaceView = (SurfaceView) findViewById(R.id.surfaceCamera);
		mSurfaceHolder = mSurfaceView.getHolder();
		mSurfaceHolder.addCallback(this);
		mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		if (mPreviewRunning) {
			mCamera.stopPreview();
		}
		Camera.Parameters p = mCamera.getParameters();
		p.setPreviewSize(width, height);
		mCamera.setParameters(p);
		try {
			mCamera.setPreviewDisplay(holder);
		} catch (IOException e) {
			e.printStackTrace();
		}
		mCamera.startPreview();
		mPreviewRunning = true;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		mCamera = Camera.open();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		mCamera.stopPreview();
		mPreviewRunning = false;
		mCamera.release();
	}

	Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {

		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			System.out.println("onPictureTaken()");
		}
	};

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.bNavigationCapturePhoto) {
			Intent i = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
			startActivityForResult(i, CAMERA_DATA);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			switch(requestCode) {
				case CAMERA_DATA:
					Bundle extras = data.getExtras();
			//		mCameraCaptureBmp = (Bitmap) extras.get("data");
					// TODO: send image to MoveSelection
			}
		}
	}

}
