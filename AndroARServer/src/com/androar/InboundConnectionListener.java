package com.androar;

import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InboundConnectionListener extends Thread {
	
	/*
	 * Returns a connection listener
	 */
	public static synchronized InboundConnectionListener getConnectionListener() {
		if (connection_listener == null) {
			connection_listener = new InboundConnectionListener();
		}
		return connection_listener;
	}
	
	// Ctor
	private InboundConnectionListener() {
		connections_thread_pool = Executors.newFixedThreadPool(num_threads);
	}
	
	/*
	 * Initializes the internal state for the Connection Listener.
	 * <p>
	 * <b>Should be called before use!</b>
	 */
	public static void Init(int port, int num_threads) {
		if (!inited) {
			InboundConnectionListener.listening_port = port;
			InboundConnectionListener.num_threads = num_threads;
			InboundConnectionListener.inited = true;
		}
	}

	public void run() {
		try {
			server_socket = new ServerSocket(listening_port);
		} catch (Exception e) {
			e.printStackTrace();
		}
		// Listen for connections_s
		Logging.LOG(Logging.CRITICAL, "Listening for connections on port " + listening_port + 
				", using maximum " + num_threads + " threads.");
		while (true) {
			try {
				for (; !stopped ;) {
					connections_thread_pool.execute(new ClientConnection(server_socket.accept()));
				}
			} catch (Exception e) {
				e.printStackTrace();
				connections_thread_pool.shutdown();
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
		return listening_port;
	}
	
	// Internal connection listener object
	private static InboundConnectionListener connection_listener;
	// Port that the connection listener listens on
	private static int listening_port;
	// Number of threads in the threadpool executor (== number of clients)
	private static int num_threads;
	// True if the user ran the Init function
	private static Boolean inited = false;
	// True if the server is stopped (all clients should terminate and we shouldn't accept any more
	// clients)
	private static Boolean stopped = false;

	// Thread pool executor
	private ExecutorService connections_thread_pool;
	// The server-side socket
	private ServerSocket server_socket;

}
