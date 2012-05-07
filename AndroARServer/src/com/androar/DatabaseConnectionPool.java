package com.androar;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class DatabaseConnectionPool {

	private static DatabaseConnectionPool singleton = null;
	private static int num_max_connections;
	private static Class<? extends IDatabaseConnection> type;
	
	private BlockingQueue<IDatabaseConnection> connections;
	
	private DatabaseConnectionPool(int num_max_connections, Class<? extends IDatabaseConnection> type) {
		connections = new ArrayBlockingQueue<IDatabaseConnection>(num_max_connections);
		for (int i = 0; i < num_max_connections; ++i) {
			try {
			if (type == CassandraDatabaseConnection.class) {
				connections.put(
						new CassandraDatabaseConnection(Constants.DATABASE_HOST,
								Constants.DATABASE_PORT));
			} else {
				connections.put(
						new MockDatabase("", 0));
			}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void Init(int num_max_connections, Class<? extends IDatabaseConnection> type) {
		DatabaseConnectionPool.num_max_connections = num_max_connections;
		DatabaseConnectionPool.type = type;
	} 
	
	public static DatabaseConnectionPool getDatabaseConnectionPool() {
		if (singleton == null) {
			singleton = new DatabaseConnectionPool(num_max_connections, type);
		}
		return singleton;
	}
	
	public IDatabaseConnection borrowConnection() throws InterruptedException {
		return connections.take();
	}
	
	public void returnConnection(IDatabaseConnection connection) {
		connections.offer(connection);
	}

}
