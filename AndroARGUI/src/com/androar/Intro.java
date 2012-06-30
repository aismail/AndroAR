package com.androar;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class Intro extends Activity {

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.intro);

		// get a thread to do our work here
		Thread thread = new Thread() {
			public void run() {
				try {
					sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} finally {
					Intent i = new Intent("com.androar.AndroAR");
					startActivity(i);
				}
			}
		};
		thread.start();
	}

	@Override
	protected void onPause() {
		super.onPause();
		finish(); // kill this activity
	}
}