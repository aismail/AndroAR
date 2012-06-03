/*
 *  Main class of the server. Contains a parser for the command-line input which sets the options
 *  and then starts the server
 */

package com.androar;

public class AndroARServer {

	/**
	 * @param args Arguments.
	 */
	public static void main(String[] args) {
		Logging.setLOGLevel(Logging.DEBUGGING);
		InboundConnectionListener.Init(6666, 10);
		DatabaseConnectionPool.Init(11, CassandraDatabaseConnection.class);
		// Let's initialize the connections here, since it might take a while
		DatabaseConnectionPool.getDatabaseConnectionPool();
		InboundConnectionListener connection_listener = 
				InboundConnectionListener.getConnectionListener();
		connection_listener.startListening();
	}

}
