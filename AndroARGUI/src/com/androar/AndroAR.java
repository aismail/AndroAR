package com.androar;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;

public class AndroAR extends Activity implements OnClickListener {
	private String classes[] = { "CameraPreview" };
	private Button b;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);
		initialize();
	}

	public void initialize() {
		b = (Button) findViewById(R.id.bCameraPreview);
		b.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		int cls = -1;

		if (v.getId() == R.id.bCameraPreview)
			cls = 0;

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
