package com.androar;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;

public class AndroAR extends Activity {
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	requestWindowFeature(Window.FEATURE_NO_TITLE);
    	setContentView(R.layout.add);
    }
}