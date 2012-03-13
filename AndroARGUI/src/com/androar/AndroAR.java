package com.androar;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;

public class AndroAR extends Activity implements OnClickListener {
	String classes[] = {"MoveSelectionActivity"};
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	requestWindowFeature(Window.FEATURE_NO_TITLE);
    	setContentView(R.layout.main);
    	initialize();
    }
	
	public void initialize() {
		Button b = (Button) findViewById(R.id.bAddData);
		b.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.bAddData) {
			try {
				Class<?> intentClass = Class.forName("com.androar." + classes[0]);
				Intent i = new Intent(this, intentClass);
				startActivity(i);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
}
