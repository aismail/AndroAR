package com.androar;

public final class Constants {
	
	private Constants() {}
	
	public static final String CASSANDRA_CLUSTER_NAME = "Test Cluster";
	public static final String CASSANDRA_KEYSPACE = "AndroARKeyspace";
	public static final String CASSANDRA_IMAGE_FEATURES_COLUMN_FAMILY = "image_features";
	public static final String CASSANDRA_OBJECT_TO_IMAGE_ASSOCIATIONS_COLUMN_FAMILY =
			"object_associations";
	public static final int CASSANDRA_REPLICATION_FACTOR = 1;
	
	public static final String DATABASE_HOST = "emerald";
	public static final int DATABASE_PORT = 9160;
}
