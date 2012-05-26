package com.androar;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.androar.comm.Communication;
import com.androar.comm.CommunicationProtos.ClientMessage;
import com.androar.comm.CommunicationProtos.ClientMessage.ClientMessageType;
import com.androar.comm.ImageFeaturesProtos.CompassPosition;
import com.androar.comm.ImageFeaturesProtos.DetectedObject;
import com.androar.comm.ImageFeaturesProtos.GPSPosition;
import com.androar.comm.ImageFeaturesProtos.Image;
import com.androar.comm.ImageFeaturesProtos.ImageContents;
import com.androar.comm.ImageFeaturesProtos.LocalizationFeatures;
import com.androar.comm.ImageFeaturesProtos.ObjectBoundingBox;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

public class CameraPreview extends Activity implements SurfaceHolder.Callback {

	// Surface View
	private SurfaceView surfaceView;
	private SurfaceHolder holder;
	// Camera Stats
	private Camera camera;
	private boolean inPreview = false;
	// Bounding boxes
	private RenderRectanglesView rectanglesView;
	// Storage
	private File pictureDir;
	// Localization Features
	private float latitude = 0, longitude = 0, azimuth = 0;
	private LocationListener locationListener;
	private LocationManager locationManager;
	private SensorManager sensorManager;
	private SensorEventListener sensorListener;
	// Communication
	private Socket socket;
	private DataInputStream in;
	private DataOutputStream out;
	// Queries
	private Camera.PictureCallback query_picture_callback = new Camera.PictureCallback() {
		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			if (socket != null && in != null && out != null) {
				long startTime = System.currentTimeMillis();
				Bitmap color_bm = BitmapFactory.decodeByteArray(data, 0,
						data.length);
				Bitmap grayscale_bm = Bitmap.createBitmap(color_bm.getWidth(),
						color_bm.getHeight(), Bitmap.Config.RGB_565);

				Canvas c = new Canvas(grayscale_bm);
				Paint p = new Paint();
				ColorMatrix cm = new ColorMatrix();

				cm.setSaturation(0);
				ColorMatrixColorFilter filter = new ColorMatrixColorFilter(cm);
				p.setColorFilter(filter);
				c.drawBitmap(color_bm, 0, 0, p);

				ByteArrayOutputStream internal_output_stream = new ByteArrayOutputStream();
				grayscale_bm.compress(CompressFormat.JPEG, 60,
						internal_output_stream);
				byte[] compressed_data = internal_output_stream.toByteArray();

				Log.d("CompressImageTime", ""
						+ (System.currentTimeMillis() - startTime));
				startTime = System.currentTimeMillis();
				// Send data to server
				sendQueryToServer(compressed_data);
				// Wait for reply
				Image annotated_image;
				try {
					annotated_image = Image.parseFrom(Communication
							.readMessage(in));
					long endTime = System.currentTimeMillis();
					long rtt = endTime - startTime;
					Log.d("RoundTripTime", "" + rtt);
					ArrayList<Rect> new_rectangles = new ArrayList<Rect>();
					for (DetectedObject detected_object : annotated_image
							.getDetectedObjectsList()) {
						ObjectBoundingBox box = detected_object
								.getBoundingBox();
						new_rectangles.add(new Rect(box.getLeft(),
								box.getTop(), box.getRight(), box.getBottom()));
					}
					// Just add another random rectangle
					int corner = (int) (Math.random() * 100);
					new_rectangles.add(new Rect(corner, corner, corner + 50,
							corner + 50));
					rectanglesView.setRects(new_rectangles);
					rectanglesView.invalidate();
				} catch (InvalidProtocolBufferException e) {
				}
			}
			camera.startPreview();
			camera.takePicture(null, null, query_picture_callback);
		}

	};
	private Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {

		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			new SavePhotoTask().execute(data);

			Intent i = new Intent(CameraPreview.this, CropOptionActivity.class);
			camera.stopPreview();
			inPreview = false;
			startActivityForResult(i, CROP_FROM_CAMERA);
		}
	};
	// Constants
	private static final int CROP_FROM_CAMERA = 2;
	private static final String HOSTNAME = "192.168.1.112";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.camera_preview);
		init();
	}

	private Image.Builder buildImageWithoutDetectedObjects(byte[] image_contents) {
		String random_hash = Double.toString(Math.random());
		// Create image
		Image.Builder image_builder = Image.newBuilder();
		image_builder.setImage(ImageContents.newBuilder()
				.setImageHash(random_hash)
				.setImageContents(ByteString.copyFrom(image_contents)).build());
		// Localization features
		GPSPosition gps_position = GPSPosition.newBuilder()
				.setLatitude(latitude).setLongitude(longitude).build();
		CompassPosition compass_position = CompassPosition.newBuilder()
				.setAngle(azimuth).build();
		image_builder.setLocalizationFeatures(LocalizationFeatures.newBuilder()
				.setGpsPosition(gps_position)
				.setCompassPosition(compass_position).build());
		return image_builder;
	}

	private void sendQueryToServer(byte[] image_contents) {
		// Client message
		ClientMessage client_message;
		client_message = ClientMessage
				.newBuilder()
				.setMessageType(ClientMessageType.IMAGE_TO_PROCESS)
				.setImageToProcess(
						buildImageWithoutDetectedObjects(image_contents)
								.build()).build();
		Communication.sendMessage(client_message, out);
	}

	public void init() {
		// We register the activity to handle the callbacks of the SurfaceView
		surfaceView = (SurfaceView) findViewById(R.id.camera_surface);
		holder = surfaceView.getHolder();
		holder.addCallback(this);
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		rectanglesView = (RenderRectanglesView) findViewById(R.id.render_rectangles);

		// Start listening for localization features
		getOrientation();

		initSocket();
	}

	private void initSocket() {
		// Init the socket for queries and stores.
		try {
			socket = new Socket(HOSTNAME, 6666);
			out = new DataOutputStream(socket.getOutputStream());
			in = new DataInputStream(socket.getInputStream());
			// Read a hello message
			Communication.readMessage(in);
			// Assume that the message was a HELLO.
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void closeSocket() {
		if (socket == null) {
			return;
		}
		try {
			socket.close();
		} catch (IOException e) {
		}
		socket = null;
		in = null;
		out = null;
	}

	@Override
	public void onResume() {
		super.onResume();
		camera = Camera.open();
		Camera.Parameters parameters = camera.getParameters();
		parameters.setJpegQuality(60);
		Camera.Size original_size = camera.getParameters().getPictureSize();
		parameters.setPictureSize(original_size.height / 4, original_size.width / 4);
		camera.setParameters(parameters);
		if (socket == null) {
			initSocket();
		}
	}

	@Override
	public void onPause() {
		camera.stopPreview();
		camera.release();
		camera = null;
		inPreview = false;
		closeSocket();
		super.onPause();
	}

	/*
	 * @returns null iff there is no preview size for this screen size.
	 */
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

	public void surfaceCreated(SurfaceHolder holder) {
		if (camera == null) {
			return;
		}
		try {
			camera.setPreviewDisplay(holder);
		} catch (Exception t) {
			Log.e("PreviewDemo-surfaceCallback",
					"Exception in setPreviewDisplay()", t);
			Toast.makeText(CameraPreview.this, t.getMessage(),
					Toast.LENGTH_LONG).show();
		}
		rectanglesView.setWillNotDraw(false);
		rectanglesView.init();
		rectanglesView.setRects(new ArrayList<Rect>());
		rectanglesView.invalidate();
		camera.startPreview();
		inPreview = true;
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		if (camera == null) {
			return;
		}
		rectanglesView.setPaintColor(Color.CYAN);
		Camera.Parameters params = camera.getParameters();
		Camera.Size size = getBestPreviewSize(width, height, params);

		if (size != null) {
			params.setPreviewSize(width, height);
			camera.setParameters(params);
		}
		// Start making queries
		camera.takePicture(null, null, query_picture_callback);

	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		try {
			locationManager.removeUpdates(locationListener);
			sensorManager.unregisterListener(sensorListener);
		} catch (IllegalArgumentException e) {
		}
	}

	/* Menu inflate. */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.preview, menu);
		return true;
	}

	void getOrientation() {
		// Acquire a reference to the system Location Manager
		locationManager = (LocationManager) this
				.getSystemService(Context.LOCATION_SERVICE);

		// Define a listener that responds to location updates
		locationListener = new LocationListener() {
			public void onLocationChanged(Location location) {
				latitude = (float) location.getLatitude();
				longitude = (float) location.getLongitude();
			}

			public void onStatusChanged(String provider, int status,
					Bundle extras) {
			}

			public void onProviderEnabled(String provider) {
			}

			public void onProviderDisabled(String provider) {
			}
		};

		// Register the listener with the Location Manager to receive location
		// updates
		//locationManager.requestLocationUpdates(
		//		LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,
				0, locationListener);

		sensorListener = new SensorEventListener() {

			@Override
			public void onSensorChanged(SensorEvent event) {
				azimuth = event.values[0];
			}

			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
			}
		};
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		sensorManager.registerListener(sensorListener,
				sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
				SensorManager.SENSOR_DELAY_GAME);
	}

	/* Add action when menu buttons pressed. */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menuCapture:
			if (inPreview) {
				camera.takePicture(null, null, mPictureCallback);
				inPreview = false;
			}
		}
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// if (resultCode != RESULT_OK) return;

		switch (requestCode) {
		case CROP_FROM_CAMERA:
			try {
				sendStoreRequestToServer();
			} catch (IOException e) {
			}
			/* Crop image is at /sdcard/Android/androAR/photoCropped.jpg. */
			File f = new File(pictureDir.getPath() + "/photoCropped.jpg");
			if (f.exists()) {
				Toast.makeText(this, "Poza a fost croppuita", Toast.LENGTH_LONG);
				f.delete();
			}
			break;
		}
	}

	public void sendStoreRequestToServer() throws IOException {
		File in_file = new File(pictureDir.getPath() + "/photo.jpg");
		FileInputStream fin = new FileInputStream(in_file);
		byte image[] = new byte[(int) in_file.length()];
		fin.read(image);

		in_file = new File(pictureDir.getPath() + "/photoCropped.jpg");
		fin = new FileInputStream(in_file);
		byte imageCropped[] = new byte[(int) in_file.length()];
		fin.read(imageCropped);

		// Create image
		Image.Builder image_builder = buildImageWithoutDetectedObjects(image);
		// Detected objects
		// We only have 1 detected object
		DetectedObject detected_object = DetectedObject
				.newBuilder()
				.setBoundingBox(
						ObjectBoundingBox.newBuilder().setTop(0).setBottom(0)
								.setLeft(0).setRight(0).build()).setId("NAME")
				.setCroppedImage(ByteString.copyFrom(imageCropped)).build();
		image_builder.addDetectedObjects(detected_object);

		// Client message
		ClientMessage client_message;
		client_message = ClientMessage.newBuilder()
				.setMessageType(ClientMessageType.IMAGES_TO_STORE)
				.addImagesToStore(image_builder.build()).build();

		Communication.sendMessage(client_message, out);
	}

	/*
	 * Class used to asynchronously save a photo to disk
	 */
	class SavePhotoTask extends AsyncTask<byte[], String, String> {
		@Override
		protected String doInBackground(byte[]... jpeg) {
			pictureDir = new File(Environment.getExternalStorageDirectory()
					.getPath() + "/Android/data/com.androar");
			if (!pictureDir.isDirectory())
				pictureDir.mkdirs();
			File photo = new File(pictureDir, "photo.jpg");
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