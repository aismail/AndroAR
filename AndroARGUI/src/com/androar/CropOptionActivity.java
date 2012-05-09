package com.androar;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.Toast;

public class CropOptionActivity extends Activity {
	private Uri mImageCaptureUri;
	private ImageView mImageView;
	private Bitmap bitmap;
	private File picturePath;
	private File pictureCroppedPath;

	private static final int CROP_FROM_CAMERA = 2;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.crop_main);
		init();
		doCrop();
	}

	public void init() {
		mImageView = (ImageView) findViewById(R.id.iv_photo);
		picturePath = new File(Environment.getExternalStorageDirectory()
				.getPath()
				+ "/Android/data/com.androar/photo.jpg");
		pictureCroppedPath = new File(Environment.getExternalStorageDirectory()
				.getPath()
				+ "/Android/data/com.androar/photoCropped.jpg");
		if (picturePath.isFile())
			bitmap = BitmapFactory.decodeFile(picturePath.getPath());
		else
			bitmap = BitmapFactory.decodeResource(getResources(),
					R.drawable.street);
	}

	private void doCrop() {
		final ArrayList<CropOption> cropOptions = new ArrayList<CropOption>();

		Intent intent = new Intent("com.android.camera.action.CROP");
		intent.setType("image/*");

		List<ResolveInfo> list = getPackageManager().queryIntentActivities(
				intent, 0);

		int size = list.size();

		if (size == 0) {
			Toast.makeText(this, "Can not find image crop app",
					Toast.LENGTH_SHORT).show();

			return;
		} else {
			mImageCaptureUri = Uri.fromFile(picturePath);
			intent.setData(mImageCaptureUri);

			intent.putExtra("outputX", 200);
			intent.putExtra("outputY", 200);
			intent.putExtra("aspectX", 1);
			intent.putExtra("aspectY", 1);
			intent.putExtra("scale", true);
			intent.putExtra("return-data", true);

			if (size == 1) {
				Intent i = new Intent(intent);
				ResolveInfo res = list.get(0);

				i.setComponent(new ComponentName(res.activityInfo.packageName,
						res.activityInfo.name));

				startActivityForResult(i, CROP_FROM_CAMERA);
			} else {
				for (ResolveInfo res : list) {
					final CropOption co = new CropOption();

					co.title = getPackageManager().getApplicationLabel(
							res.activityInfo.applicationInfo);
					co.icon = getPackageManager().getApplicationIcon(
							res.activityInfo.applicationInfo);
					co.appIntent = new Intent(intent);

					co.appIntent
							.setComponent(new ComponentName(
									res.activityInfo.packageName,
									res.activityInfo.name));

					cropOptions.add(co);
				}

				CropOptionAdapter adapter = new CropOptionAdapter(
						getApplicationContext(), cropOptions);

				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("Choose Crop App");
				builder.setAdapter(adapter,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int item) {
								startActivityForResult(
										cropOptions.get(item).appIntent,
										CROP_FROM_CAMERA);
							}
						});

				builder
						.setOnCancelListener(new DialogInterface.OnCancelListener() {
							@Override
							public void onCancel(DialogInterface dialog) {

								if (mImageCaptureUri != null) {
									getContentResolver().delete(
											mImageCaptureUri, null, null);
									mImageCaptureUri = null;
								}
							}
						});

				AlertDialog alert = builder.create();

				alert.show();
			}
		}
	}

	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) return;
 
        switch (requestCode) {
 
        	case CROP_FROM_CAMERA:
                Bundle extras = data.getExtras();
 
                if (extras != null) {
                	/* Get cropped photo. */
                    Bitmap photo = extras.getParcelable("data");
//                  mImageView.setImageBitmap(photo);
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    photo.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    byte[] byteArray = stream.toByteArray();

        			try {
        				FileOutputStream fos = new FileOutputStream(pictureCroppedPath);

        				fos.write(byteArray);
        				fos.close();
        			} catch (java.io.IOException e) {
        				e.printStackTrace();
        			}
                }
 
//                File f = new File(mImageCaptureUri.getPath());
// 
//                if (f.exists()) f.delete();
 
                finish();
//        		finishActivity(RESULT_OK);
        }
	}
}