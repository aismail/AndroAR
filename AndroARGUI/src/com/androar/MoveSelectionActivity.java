package com.androar;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.Window;
import android.widget.Toast;

import com.androar.comm.Communication;
import com.androar.comm.CommunicationProtos.ClientMessage.ClientMessageType;
import com.androar.comm.Mocking;
import com.androar.comm.CommunicationProtos.ClientMessage;
import com.androar.comm.CommunicationProtos.ServerMessage;

public class MoveSelectionActivity extends Activity implements
		SurfaceHolder.Callback {
	private SurfaceHolder mSurfaceHolder;
	private MoveSelection mSelectionView;
	private DrawThread mThread = null;
	private Context context_ = null;
	private final int DEFAULT_SELECTION_SIZE = 200;
	private float x, y;
	private Bitmap bitmap = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.add);
		initialize(savedInstanceState);
	}

	public void initialize(Bundle savedInstanceState) {
		mSelectionView = (MoveSelection) findViewById(R.id.MoveSelectionView);
		mSurfaceHolder = mSelectionView.getHolder();
		mSurfaceHolder.addCallback(this);
	}

	public void initResources() {
		// default selection
		if (bitmap == null) {
			bitmap = BitmapFactory.decodeResource(getResources(),
					R.drawable.ic_launcher);
			mSelectionView.setSelection(bitmap);
		}

		try {
			byte[] b = getIntent().getByteArrayExtra("data");
			bitmap = BitmapFactory.decodeByteArray(b, 0, b.length);
		} catch (NullPointerException e) {
			bitmap = BitmapFactory.decodeResource(getResources(),
					R.drawable.street);
		} finally {
			mSelectionView.setBackground(bitmap);
		}

		mSelectionView.resizeBitmap(DEFAULT_SELECTION_SIZE,
				DEFAULT_SELECTION_SIZE);
		mThread = new DrawThread(mSurfaceHolder, mSelectionView);
		// Just send a mock protocol buffer
		// sendMockPB();
	}

	private void sendMockPB() {
		Socket socket;
		DataOutputStream out;
		DataInputStream in;

		try {
			socket = new Socket("192.168.100.112", 6666);
			out = new DataOutputStream(socket.getOutputStream());
			in = new DataInputStream(socket.getInputStream());

			// Read a message
			ServerMessage server_message = ServerMessage
					.parseFrom(Communication.readMessage(in));
			Log.i("PB", "***\n " + server_message.toString() + "\n***");
			Toast.makeText(context_, "***\n " + server_message.toString()
					+ "\n***", Toast.LENGTH_LONG);

			// Assume that the message was a HELLO. Let's now send an image to
			// see if this works.
			// We will read an image stored on the Hard Drive for now, it's path
			// is being passed through params
			InputStream fin = context_.getResources().openRawResource(
					R.drawable.street);
			byte file_contents[] = new byte[fin.available()];
			fin.read(file_contents);

			List<String> objects = new ArrayList<String>();
			objects.add("OBJ");
			Mocking.setMetadata("hash", objects, 44, 61);
			ClientMessage client_message = Mocking
					.createMockClientMessage(file_contents, ClientMessageType.IMAGES_TO_STORE);
			Log.i("PB", "***\n " + client_message.toString() + "\n***");

			Communication.sendMessage(client_message, out);

			socket.close();
		} catch (Exception e) {
			e.printStackTrace();
			Log.e("PB", e.getMessage());
		}
		return;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		initResources();
		x = holder.getSurfaceFrame().exactCenterX();
		y = holder.getSurfaceFrame().exactCenterY();
		mSelectionView.setCoords(x, y);
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
			} catch (InterruptedException e) {
			}
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
	}

}
