package com.androar;

import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InboundConnectionListener extends Thread {
	
	/*
	 * Returns a connection listener
	 */
	public static synchronized InboundConnectionListener getConnectionListener() {
		if (connectionListener == null) {
			connectionListener = new InboundConnectionListener();
		}
		return connectionListener;
	}
	
	// Ctor
	private InboundConnectionListener() {
		connectionsThreadPool = Executors.newFixedThreadPool(numThreads);
	}
	
	/*
	 * Initializes the internal state for the Connection Listener.
	 * <p>
	 * <b>Should be called before use!</b>
	 */
	public static void Init(int port, int numThreads) {
		if (!inited) {
			InboundConnectionListener.listeningPort = port;
			InboundConnectionListener.numThreads = numThreads;
			InboundConnectionListener.inited = true;
		}
	}

	public void run() {
		try {
			serverSocket = new ServerSocket(listeningPort);
		} catch (Exception e) {
			e.printStackTrace();
		}
		// Listen for connections_s
		Logging.LOG(0, "Listening for connections on port " + listeningPort + 
				", using maximum " + numThreads + " threads.");
		while (true) {
			try {
				for (; !stopped ;) {
					connectionsThreadPool.execute(new ClientConnection(serverSocket.accept()));
				}
			} catch (Exception e) {
				connectionsThreadPool.shutdown();
			}
		}
	}
	
	public void startListening() {
		this.start();
	}
	
	/*
	 * Returns the port that the clients are requesting a connection on.
	 */
	public static int getListeningPort() {
		return listeningPort;
	}
	
	// Internal connection listener object
	private static InboundConnectionListener connectionListener;
	// Port that the connection listener listens on
	private static int listeningPort;
	// Number of threads in the threadpool executor (== number of clients)
	private static int numThreads;
	// True if the user ran the Init function
	private static Boolean inited = false;
	// True if the server is stopped (all clients should terminate and we shouldn't accept any more
	// clients)
	private static Boolean stopped = false;

	// Thread pool executor
	private ExecutorService connectionsThreadPool;
	// The server-side socket
	private ServerSocket serverSocket;

}
