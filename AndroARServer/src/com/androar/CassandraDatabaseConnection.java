package com.androar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.service.ThriftKsDef;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.ddl.ColumnFamilyDefinition;
import me.prettyprint.hector.api.ddl.KeyspaceDefinition;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.ColumnQuery;
import me.prettyprint.hector.api.query.QueryResult;

import com.androar.comm.ImageFeaturesProtos.DetectedObject;
import com.androar.comm.ImageFeaturesProtos.Image;
import com.androar.comm.ImageFeaturesProtos.LocalizationFeatures;

public class CassandraDatabaseConnection implements IDatabaseConnection {

	private StringSerializer string_serializer = StringSerializer.get();
	
	private String hostname;
	private int port;
	private Cluster cluster;
	private Keyspace keyspace_operator;
	
	public CassandraDatabaseConnection(String hostname, int port) {
		this.hostname = hostname;
		this.port = port;
		
		// Connect to a cluster
		cluster = HFactory.getOrCreateCluster(
				Constants.CASSANDRA_CLUSTER_NAME, hostname + ":" + Integer.toString(port));
		if (cluster.describeKeyspace(Constants.CASSANDRA_KEYSPACE) == null) {
			createSchema(cluster);
		}
		keyspace_operator = HFactory.createKeyspace(Constants.CASSANDRA_KEYSPACE, cluster);
	}

	public void closeConnection() {
		Logging.LOG(3, "Closing Cassandra connection");
		cluster.getConnectionManager().shutdown();
	}
	
	private static void createSchema(Cluster cluster) {
		ColumnFamilyDefinition image_features_column_family_definition =
				HFactory.createColumnFamilyDefinition(Constants.CASSANDRA_KEYSPACE,
						Constants.CASSANDRA_IMAGE_FEATURES_COLUMN_FAMILY);
		ColumnFamilyDefinition object_associations_column_family_definition =
				HFactory.createColumnFamilyDefinition(Constants.CASSANDRA_KEYSPACE,
						Constants.CASSANDRA_OBJECT_TO_IMAGE_ASSOCIATIONS_COLUMN_FAMILY);

		List<ColumnFamilyDefinition> all_column_families = new ArrayList<ColumnFamilyDefinition>();
		all_column_families.add(image_features_column_family_definition);
		all_column_families.add(object_associations_column_family_definition);
		
		KeyspaceDefinition new_keyspace = HFactory.createKeyspaceDefinition(
				Constants.CASSANDRA_KEYSPACE,
				ThriftKsDef.DEF_STRATEGY_CLASS,
				Constants.CASSANDRA_REPLICATION_FACTOR,
				all_column_families);
		// Add the schema to the cluster.
		Logging.LOG(3, "Creating Cassandra keyspace " + Constants.CASSANDRA_KEYSPACE);
		cluster.addKeyspace(new_keyspace, false /* Hector won't block */);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.androar.IDatabaseConnection#storeImage(com.androar.comm.ImageFeaturesProtos.Image)
	 */
	@Override
	public boolean storeImage(Image image) {
		Mutator<String> mutator = HFactory.createMutator(keyspace_operator, string_serializer);
		// Store image features for this image:
		// * all detected objects, in columns "objectX";
		// * image contents
		// * localization features (toString() of protocol buffer)
		//   - GPS latitude
		//   - GPS longitude
		//   - compass orientation
		// The row key is the image hash.
		String image_hash = ImageUtils.computeImageHash(image);
		Logging.LOG(10, "Image Hash is: " + image_hash);
		// Detected Objects
		for (int object = 0; object < image.getDetectedObjectsCount(); ++object) {
			DetectedObject detected_object = image.getDetectedObjects(object);
			Logging.LOG(10, "Storing detected object: " + detected_object.getName());
			mutator.insert(image_hash,
					Constants.CASSANDRA_IMAGE_FEATURES_COLUMN_FAMILY,
					HFactory.createStringColumn("object" + object,
							detected_object.getName()));
		}
		// Image Contents
		Logging.LOG(10, 
				"Storing image contents: " + image.getImage().getImageContents().toString());
		mutator.insert(image_hash,
				Constants.CASSANDRA_IMAGE_FEATURES_COLUMN_FAMILY,
				HFactory.createStringColumn("image_contents",
						image.getImage().getImageContents().toString()));
		// Localization Features
		Logging.LOG(10, 
				"Storing image features: " + image.getLocalizationFeatures().toString());
		mutator.insert(image_hash,
				Constants.CASSANDRA_IMAGE_FEATURES_COLUMN_FAMILY,
				HFactory.createStringColumn("localization",
						image.getLocalizationFeatures().toString()));
		// Store image hash for each detected object
		for (int object = 0; object < image.getDetectedObjectsCount(); ++object) {
			DetectedObject detected_object = image.getDetectedObjects(object);
			// If the detected object doesn't exist yet, then we should add:
			// * all its metadata;
			// * a first_available_image_id tag
			int first_unused_image_id;
			if (!detectedObjectExists(detected_object.getName())) {
				mutator.insert(detected_object.getName(),
						Constants.CASSANDRA_OBJECT_TO_IMAGE_ASSOCIATIONS_COLUMN_FAMILY,
						HFactory.createStringColumn("first_available_image_id", "0"));
				first_unused_image_id = 0;
			} else {
				first_unused_image_id = 
						getDetectedObjectFirstAvailableImageId(detected_object.getName());
			}
			// We insert a reference to our image
			mutator.insert(detected_object.getName(),
					Constants.CASSANDRA_OBJECT_TO_IMAGE_ASSOCIATIONS_COLUMN_FAMILY,
					HFactory.createStringColumn(
							"image_hash" + Integer.toString(first_unused_image_id),
							image_hash));
			// We change the value of the first available image id column
			mutator.insert(detected_object.getName(),
					Constants.CASSANDRA_OBJECT_TO_IMAGE_ASSOCIATIONS_COLUMN_FAMILY,
					HFactory.createStringColumn("first_available_image_id",
							Integer.toString(first_unused_image_id + 1)));
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see com.androar.IDatabaseConnection#getImagesInRange(com.androar.comm.ImageFeaturesProtos.LocalizationFeatures, double)
	 */
	@Override
	public List<Image> getImagesInRange(LocalizationFeatures position,
			double range) {
		// TODO Auto-generated method stub
		return null;
	}
	

	private int getDetectedObjectFirstAvailableImageId(String object_name) {
		ColumnQuery<String, String, String> column_query = 
				HFactory.createStringColumnQuery(keyspace_operator);
        column_query
        		.setColumnFamily(Constants.CASSANDRA_OBJECT_TO_IMAGE_ASSOCIATIONS_COLUMN_FAMILY)
        		.setKey(object_name).setName("first_available_image_id");
        QueryResult<HColumn<String, String>> result = column_query.execute();
        return Integer.parseInt(result.get().getValue());
	}

	private boolean detectedObjectExists(String object_name) {
		ColumnQuery<String, String, String> column_query = 
				HFactory.createStringColumnQuery(keyspace_operator);
        column_query
        		.setColumnFamily(Constants.CASSANDRA_OBJECT_TO_IMAGE_ASSOCIATIONS_COLUMN_FAMILY)
        		.setKey(object_name).setName("first_available_image_id");
        QueryResult<HColumn<String, String>> result = column_query.execute();
        Logging.LOG(10, "Object Query (looking for <first_available_image_id> column: \n" + 
        		result.get().toString());
        return (result != null);
	}

}
