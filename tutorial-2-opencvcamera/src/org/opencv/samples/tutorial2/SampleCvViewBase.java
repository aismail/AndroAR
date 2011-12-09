package org.opencv.samples.tutorial2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;

import org.opencv.core.Size;
import org.opencv.highgui.VideoCapture;
import org.opencv.highgui.Highgui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public abstract class SampleCvViewBase extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private static final String TAG = "Sample::SurfaceView";

    private SurfaceHolder       mHolder;
    private VideoCapture        mCamera;

    public SampleCvViewBase(Context context) {
        super(context);
        mHolder = getHolder();
        mHolder.addCallback(this);
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    public void surfaceChanged(SurfaceHolder _holder, int format, int width, int height) {
        Log.i(TAG, "surfaceCreated");
        synchronized (this) {
            if (mCamera != null && mCamera.isOpened()) {
                Log.i(TAG, "before mCamera.getSupportedPreviewSizes()");
                List<Size> sizes = mCamera.getSupportedPreviewSizes();
                Log.i(TAG, "after mCamera.getSupportedPreviewSizes()");
                int mFrameWidth = width;
                int mFrameHeight = height;

                // selecting optimal camera preview size
                {
                    double minDiff = Double.MAX_VALUE;
                    for (Size size : sizes) {
                        if (Math.abs(size.height - height) < minDiff) {
                            mFrameWidth = (int) size.width;
                            mFrameHeight = (int) size.height;
                            minDiff = Math.abs(size.height - height);
                        }
                    }
                }
                
                mCamera.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, mFrameWidth);
                mCamera.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, mFrameHeight);
            }
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "surfaceCreated");
        
        mCamera = new VideoCapture(Highgui.CV_CAP_ANDROID);
        if (mCamera.isOpened()) {
        	(new Thread(this)).start();
        } else {
        	mCamera.release();
        	mCamera = null;
        	Log.e(TAG, "Failed to open native camera");
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "surfaceDestroyed");
        if (mCamera != null) {
            synchronized (this) {
                mCamera.release();
                mCamera = null;
            }
        }
    }

    protected abstract Bitmap processFrame(VideoCapture capture);

    public void run() {
        Log.i(TAG, "Starting processing thread");
        
        InetAddress serverAddr = null;
        Socket socket = null;
        InputStreamReader sockIn = null;
        OutputStream sockOut = null;
         
		try {
			serverAddr = InetAddress.getByName("192.168.100.123");
			socket = new Socket(serverAddr, 3333);
			socket.setSoTimeout(100000);
			sockIn = new InputStreamReader(socket.getInputStream());
			sockOut = socket.getOutputStream();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
        Log.d("Networking", "C: Connecting...");
        
       
        
        
        while (true) {
            Bitmap bmp = null;
 
            synchronized (this) {
                if (mCamera == null)
                    break;
 
                if (!mCamera.grab()) {
                    Log.e(TAG, "mCamera.grab() failed");
                    break;
                }

                bmp = processFrame(mCamera);
                 
                
                try {
                	char[] stamp = new char[1024];
                	/*
                	int rc = sockIn.read(stamp);
                	Log.d("XXX", stamp.toString())
                	 // Write image to socket
                    bmp.compress(Bitmap.CompressFormat.JPEG, 50, socket.getOutputStream());
                    // Wait for ok
                    
					//sockOut.write(stamp, 0, 8);
					sockOut.flush();
					*/
                	
                	ByteArrayOutputStream stream = new ByteArrayOutputStream();
                	bmp.compress(Bitmap.CompressFormat.JPEG, 50, stream);
                	byte[] input = stream.toByteArray();
                	byte b1, b2, b3, b4; 
                	b1 = (byte) (input.length);
                	b2 = (byte) (input.length >> 8); 
                	b3 = (byte) (input.length >> 16);
                	b4 = (byte) (input.length >> 24);
                	Log.d("LEN", Integer.toString(input.length) + ": " + b4 + " " + b3 + " " + b2 + " " + b1);
                	sockOut.write(b1);
                	sockOut.write(b2);
                	sockOut.write(b3);
                	sockOut.write(b4);
                	sockOut.flush();
                	sockOut.write(input); 
                	sockOut.flush();
                	
                	int obj = sockIn.read();
                	Canvas c = new Canvas(bmp);
                	Paint p = new Paint();
                	p.setColor(Color.RED);
                	p.setTextSize(40);
                	
                	if((char)obj == 127)
                		c.drawText("Detection off", 100.0f, 100.0f, p);
                	else if((char) obj == 126)
                		c.drawText("Object Not Detected", 100.0f, 100.0f, p);
                	else {
                		p.setColor(Color.GREEN);
                		c.drawText("Detected Object: " + obj, 100.0f, 100.0f, p);
                	}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }

            if (bmp != null) {
                Canvas canvas = mHolder.lockCanvas();
                if (canvas != null) {
                    canvas.drawBitmap(bmp, (canvas.getWidth() - bmp.getWidth()) / 2, (canvas.getHeight() - bmp.getHeight()) / 2, null);
                    mHolder.unlockCanvasAndPost(canvas);
                }
                bmp.recycle();
            }
        }

        Log.i(TAG, "Finishing processing thread");
    }
}
