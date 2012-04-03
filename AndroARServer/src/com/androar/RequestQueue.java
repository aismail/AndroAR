package com.androar;

import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;

import com.androar.comm.Communication;

public class RequestQueue {

	private BlockingQueue<Request> queue;
	private Socket opencv_socket;
	
	private static RequestQueue request_queue_internal = null;
	
	public static RequestQueue getRequestQueue() {
		if (request_queue_internal == null) {
			request_queue_internal = new RequestQueue();
		}
		return request_queue_internal;
	}
	
	private RequestQueue() {
		queue = new SynchronousQueue<Request>();
		try {
			opencv_socket = new Socket(Constants.OPENCV_HOST, Constants.OPENCV_PORT);
		} catch (Exception e) {
			e.printStackTrace();
		}
		Thread thread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				// Get request from queue
				Request request = queue.poll();
				// Transmit message, get reply and execute callback
				Object response = Communication.sendAndProcessRequestToOpenCV(
						request.getRequestToByteArray(), opencv_socket);
				// Execute callback
				request.callCallback(response);
			}
		});
		thread.run();
	}
	
	public void newRequest(Request request) {
		// Put request in queue
		queue.add(request);
	}
	
}
