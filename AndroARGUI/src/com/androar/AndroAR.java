package com.androar;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;

public class AndroAR extends Activity implements OnClickListener {
	private String classes[] = { "MoveSelectionActivity", "NavigationActivity",
			"CameraPreview", "CropOptionActivity" };
	private Button b1, b2, b3, b4;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);
		initialize();
	}

	public void initialize() {
		b1 = (Button) findViewById(R.id.bAddData);
		b2 = (Button) findViewById(R.id.bNavigateActivity);
		b3 = (Button) findViewById(R.id.bCameraPreview);
		b4 = (Button) findViewById(R.id.bCropImage);
		b1.setOnClickListener(this);
		b2.setOnClickListener(this);
		b3.setOnClickListener(this);
		b4.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		int cls = -1;
		if (v.getId() == R.id.bAddData) {
			cls = 0;
		} else if (v.getId() == R.id.bNavigateActivity) {
			cls = 1;
		} else if (v.getId() == R.id.bCameraPreview) {
			cls = 2;
		} else if (v.getId() == R.id.bCropImage) {
			cls = 3;
		}
		if (cls != -1)
			try {
				Class<?> intentClass = Class.forName("com.androar."
						+ classes[cls]);
				Intent i = new Intent(this, intentClass);
				startActivity(i);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
	}
}
