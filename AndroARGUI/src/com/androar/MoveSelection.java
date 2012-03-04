package com.androar;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.Socket;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.androar.comm.Communication;
import com.androar.comm.CommunicationProtos.AuthentificationInfo;
import com.androar.comm.CommunicationProtos.ClientMessage;
import com.androar.comm.CommunicationProtos.ServerMessage;
import com.androar.comm.CommunicationProtos.ClientMessage.ClientMessageType;
import com.androar.comm.ImageFeaturesProtos.DetectedObject;
import com.androar.comm.ImageFeaturesProtos.Image;
import com.androar.comm.ImageFeaturesProtos.ImageContents;
import com.androar.comm.ImageFeaturesProtos.ObjectBoundingBox;
import com.androar.comm.ImageFeaturesProtos.DetectedObject.DetectedObjectType;
import com.google.protobuf.ByteString;

public class MoveSelection extends SurfaceView implements SurfaceHolder.Callback {
	private SurfaceHolder mSurfaceHolder;
	private DrawThread mThread = null;
	private Bitmap bitmap, background;
	private float x = 0, y = 0;
	private Context context_ = null;
	
	// selection modes
	private final int NONE = 0;
	private final int DRAG = 1;
	private final int PINCH = 2;
	private int MODE = NONE;

	private final int DEFAULT_SELECTION_SIZE = 200;
	private final float DEFAULT_EPSILON = (float) 0.5;

	private class ImageParams {
		// TODO: encapsulation stuff here
		float epsilon = DEFAULT_EPSILON; // error of approximation

		public float euclidianDist(float x1, float y1, float x2, float y2) {
			float dx = x1 - x2;
			float dy = y1 - y2;
			return (float) Math.sqrt(dx * dx + dy * dy);
		}
	}

	/**
	 * Subclass of SurfaceView. Moves a bitmap on the SurfaceView by
	 * permanently redrawing it, using threads.
	 */
	public MoveSelection(Context context) {
		super(context);
		context_ = context;
		initSurface();
	}

	/**
	 * Subclass of SurfaceView. Moves a bitmap on the SurfaceView by
	 * permanently redrawing it, using threads.
	 */
	public MoveSelection(Context context, AttributeSet attrs) {
		super(context, attrs);
		context_ = context;
		initSurface();
	}

	/**
	 * Subclass of SurfaceView. Moves a bitmap on the SurfaceView by
	 * permanently redrawing it, using threads.
	 */
	public MoveSelection(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		context_ = context;
		initSurface();
	}
	
	private void sendMockPB() {
		Socket socket;
		DataOutputStream out;
        DataInputStream in;
        
		try {
			socket = new Socket("192.168.100.104", 6666);
			out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());
            
            // Read a message
            ServerMessage server_message = ServerMessage.parseFrom(Communication.readMessage(in));
            Log.i("PB", "***\n " + server_message.toString() + "\n***");
            Toast.makeText(context_, "***\n " + server_message.toString() + "\n***", Toast.LENGTH_LONG);
            
            // Assume that the message was a HELLO. Let's now send an image to see if this works.
            // We will read an image stored on the Hard Drive for now, it's path is being passed through params
            InputStream fin = context_.getResources().openRawResource(R.drawable.street);
            byte file_contents[] = new byte[fin.available()];
            fin.read(file_contents);
            
            ByteString image_contents = ByteString.copyFrom(file_contents);
            
            Image image = Image.newBuilder().
            	addDetectedObjects(
            		DetectedObject.newBuilder().
            		setObjectType(DetectedObjectType.BUILDING).
            		setName("OBJECT_1").
            		setBoundingBox(
            			ObjectBoundingBox.newBuilder().
            			setTop(0).
            			setBottom(100).
            			setLeft(0).
            			setRight(100).
            			build()).
            		build()).
            	setImage(
            		ImageContents.newBuilder().
            		setImageHash("IMAGE_HASH").
            		setImageContents(image_contents)).
            	build();
            
            ClientMessage client_message = ClientMessage.newBuilder()
            	.setAuthentificationInfo(
            		AuthentificationInfo.newBuilder()
            		.setPhoneId("PHONE_ID")
            		.setHash("CURRENT_HASH_OF_PHONE_ID")
            		.build())
            	.setMessageType(ClientMessageType.IMAGE_TO_PROCESS)
            	.setImageToProcess(image)
            	.build();
            
            Log.i("PB", "***\n " + client_message.toString() + "\n***");
            
            Communication.sendMessage(client_message, out);
            
            socket.close();
		} catch (Exception e) {
			e.printStackTrace();
			Log.e("PB", e.getMessage());
		}
		return;
	}
	
	public void initSurface() {
		mSurfaceHolder = getHolder();
		mSurfaceHolder.addCallback(this);
	}
	
	public void initResources() {
		bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
		resizeBitmap(DEFAULT_SELECTION_SIZE, DEFAULT_SELECTION_SIZE);
		background = BitmapFactory.decodeResource(getResources(), R.drawable.street);
		mThread = new DrawThread(mSurfaceHolder, this);
		setFocusable(true);
		// Just send a mock protocol buffer
//		sendMockPB();
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		Rect oldR = new Rect(0, 0, background.getWidth(), background.getHeight());
		Rect newR = new Rect(0, 0, this.getWidth(), this.getHeight());
		canvas.drawBitmap(background, oldR, newR, null);
		canvas.drawBitmap(bitmap, x - (bitmap.getWidth() / 2), y - (bitmap.getHeight() / 2), null);
	}
	
	/* Resize a bitmap to the newWidth 
	 * and newHeight using a Matrix
	 */
	public void resizeBitmap(int newWidth, int newHeight) {
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		
		float scaleW = ((float) newWidth) / width;
		float scaleH = ((float) newHeight) / height;
		
		Matrix matrix = new Matrix();
		matrix.postScale(scaleW, scaleH);
		// resize the bitmap now
		bitmap = Bitmap.createBitmap(bitmap, (int) 0, (int) 0, width, height,
				matrix, true);
	}

	/*
	 * Move the bitmap only if the first click was done on it, and then moved.
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		float off_x = event.getX();
		float off_y = event.getY();
		float oldDist;
		ImageParams image = new ImageParams();

		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_POINTER_UP:
			if (MODE == PINCH) {
				MODE = DRAG;
			}
			break;
		case MotionEvent.ACTION_UP:
			if (MODE == DRAG) {
				x = off_x;
				y = off_y;
			}
			MODE = NONE;
			break;
		case MotionEvent.ACTION_DOWN:
			if ((Math.abs(off_x - x) <= bitmap.getWidth() / 2)
					&& (Math.abs(off_y - y) <= bitmap.getHeight() / 2))
				MODE = DRAG;
			break;
		case MotionEvent.ACTION_MOVE:
			if (MODE == DRAG) {
				x = off_x;
				y = off_y;
			} else if (MODE == PINCH) {
				float newDist = image.euclidianDist(event.getX(0), event
						.getY(0), event.getX(1), event.getY(1));
				if (newDist > 10f)
					resizeBitmap((int) newDist, (int) newDist);
			}
			break;
		case MotionEvent.ACTION_POINTER_DOWN:
			oldDist = image.euclidianDist(event.getX(0), event.getY(0), event
					.getX(1), event.getY(1));
			if (oldDist > 10f)
				MODE = PINCH;
			break;
		}
		return true;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		initResources();
		x = holder.getSurfaceFrame().exactCenterX();
		y = holder.getSurfaceFrame().exactCenterY();
		mThread.setRunning(true);
		mThread.start();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		boolean retry = true;
		mThread.setRunning(false);
		while (retry) {
			try {
				mThread.join();
				retry = false;
			} catch (InterruptedException e) {}
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {}

	/**
	 * Thread for permanently redrawing the SurfaceView.
	 */
	class DrawThread extends Thread {
		private SurfaceHolder surfaceHolder;
		private MoveSelection panel;
		private boolean run;

		public DrawThread(SurfaceHolder surfaceHolder, MoveSelection panel) {
			this.surfaceHolder = surfaceHolder;
			this.panel = panel;
		}

		public void setRunning(boolean run) {
			this.run = run;
		}

		@Override
		public void run() {
			Canvas c;
			while (run) {
				c = null;
				try {
					c = surfaceHolder.lockCanvas();
					synchronized (surfaceHolder) {
						panel.onDraw(c);
					}
				} finally {
					if (c != null)
						surfaceHolder.unlockCanvasAndPost(c);
				}
			}
		}
	}
}