package com.androar;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;

public class LabelCroppedImage extends Activity {
	ImageView imageView;
	String imageInSD;
	Bitmap picture;

	@Override
	protected void onCreate(Bundle iCicle) {
		// TODO Auto-generated method stub
		super.onCreate(iCicle);
		setContentView(R.layout.save_cropped);
		config(iCicle);
	}
	
	void config(Bundle iCicle) {
		imageView = (ImageView) findViewById(R.id.ivCroppedPicture);
		imageInSD = CameraPreview.getPictureDir() + "/photoCropped.jpg";
		picture = BitmapFactory.decodeFile(imageInSD);
		imageView.setImageBitmap(picture);
	}
}
