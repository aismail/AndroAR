package com.androar;

import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.androar.comm.Communication;

public class RequestQueue {

	private BlockingQueue<Request> queue;
	private Socket opencv_socket;
	
	private static RequestQueue request_queue_internal = null;
	
	public static synchronized RequestQueue getRequestQueue() {
		if (request_queue_internal == null) {
			request_queue_internal = new RequestQueue();
		}
		return request_queue_internal;
	}
	
	private RequestQueue() {
		queue = new LinkedBlockingQueue<Request>();
		try {
			opencv_socket = new Socket(Constants.OPENCV_HOST, Constants.OPENCV_PORT);
		} catch (Exception e) {
			e.printStackTrace();
		}
		Thread thread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				while (true) {
					// Get request from queue
					Request request;
					try {
						request = queue.take();
						// Transmit message, get reply and execute callback
						byte[] response = Communication.sendAndProcessRequestToOpenCV(
								request.getRequestToByteArray(), opencv_socket);
						// Execute callback
						request.callCallback(response);
					} catch (InterruptedException e) {
					}
				}
			}
		});
		thread.start();
	}
	
	public synchronized void newRequest(Request request) {
		// Put request in queue
		Logging.LOG(0, "Queued request for image " + ImageUtils.computeImageHash(request.getImage()));
		queue.add(request);
	}
	
}
