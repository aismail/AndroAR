// Main class of the server. Contains a parser for the command-line input which sets the options
// and then starts the server

package com.androar;

public class AndroARServer {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		InboundConnectionListener.Init(6666, 10);
		InboundConnectionListener connectionListener = InboundConnectionListener.getConnectionListener();
		connectionListener.startListening();
	}

}
