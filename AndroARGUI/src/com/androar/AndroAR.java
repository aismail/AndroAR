package com.androar;

import android.app.Activity;
import android.os.Bundle;

public class AndroAR extends Activity {
	MoveSelection view;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	view = new MoveSelection(this);
    	view.setOnTouchListener(view);
    	setContentView(view);
    }
	
	@Override
	protected void onPause() {
		super.onPause();
		view.pause();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		view.resume();
	}
}