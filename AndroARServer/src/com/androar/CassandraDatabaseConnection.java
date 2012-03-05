package com.androar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.prettyprint.cassandra.serializers.BytesArraySerializer;
import me.prettyprint.cassandra.serializers.IntegerSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.serializers.TypeInferringSerializer;
import me.prettyprint.cassandra.service.ThriftKsDef;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.ddl.ColumnFamilyDefinition;
import me.prettyprint.hector.api.ddl.ColumnType;
import me.prettyprint.hector.api.ddl.KeyspaceDefinition;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.ColumnQuery;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.SubColumnQuery;
import me.prettyprint.hector.api.query.SuperColumnQuery;

import com.androar.comm.ImageFeaturesProtos.DetectedObject;
import com.androar.comm.ImageFeaturesProtos.Image;
import com.androar.comm.ImageFeaturesProtos.ImageContents;
import com.androar.comm.ImageFeaturesProtos.LocalizationFeatures;

public class CassandraDatabaseConnection implements IDatabaseConnection {

	private static StringSerializer string_serializer = StringSerializer.get();
	private static BytesArraySerializer bytearray_serializer = BytesArraySerializer.get();
	private static IntegerSerializer integer_serializer = IntegerSerializer.get();
	
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
		image_features_column_family_definition.setColumnType(ColumnType.STANDARD);
		ColumnFamilyDefinition image_features_indexed_by_gps_column_family_definition =
				HFactory.createColumnFamilyDefinition(Constants.CASSANDRA_KEYSPACE,
						Constants.CASSANDRA_IMAGE_FEATURES_INDEXED_BY_GPS_COLUMN_FAMILY);
		image_features_indexed_by_gps_column_family_definition.setColumnType(ColumnType.STANDARD);
		ColumnFamilyDefinition object_associations_column_family_definition =
				HFactory.createColumnFamilyDefinition(Constants.CASSANDRA_KEYSPACE,
						Constants.CASSANDRA_OBJECT_TO_IMAGE_ASSOCIATIONS_COLUMN_FAMILY);
		object_associations_column_family_definition.setColumnType(ColumnType.SUPER);
		List<ColumnFamilyDefinition> all_column_families = new ArrayList<ColumnFamilyDefinition>();
		all_column_families.add(image_features_column_family_definition);
		all_column_families.add(image_features_indexed_by_gps_column_family_definition);
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
		// IMAGE_FEATURES column family
		// * image contents (Column)
		// * localization features (toString() of protocol buffer) (Column)
		//   - GPS latitude
		//   - GPS longitude
		//   - compass orientation 
		// * number of detected objects
		// * all detected objects' ids, in columns "objectX" (Column)
		// The row key is the image hash / id.
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
		mutator.insert(image_hash,
				Constants.CASSANDRA_IMAGE_FEATURES_COLUMN_FAMILY, 
				HFactory.createColumn("num_objects", image.getDetectedObjectsCount(),
						string_serializer, integer_serializer));
		// Image Contents
		Logging.LOG(10, 
				"Storing image contents: " + image.getImage().getImageContents().toByteArray());
		mutator.insert(image_hash,
				Constants.CASSANDRA_IMAGE_FEATURES_COLUMN_FAMILY,
				HFactory.createColumn("image_contents",
						image.getImage().getImageContents().toByteArray(),
						string_serializer, bytearray_serializer));
		// Localization Features
		Logging.LOG(10, 
				"Storing image features: " + image.getLocalizationFeatures().toString());
		mutator.insert(image_hash,
				Constants.CASSANDRA_IMAGE_FEATURES_COLUMN_FAMILY,
				HFactory.createStringColumn("localization",
						image.getLocalizationFeatures().toString()));
		// IMAGE_FEATURES_INDEXED_BY_GPS column family
		// * image_hash (Column)
		// The row key is the gps position
		// TODO(alex): add this here, indexed by gps
		
		// OBJECTS_TO_IMAGE_ASSOCIATIONS super column family
		// Since we can't put both standard columns (such as name, description) and super columns
		// (such as images that contain the objects (with gps position, cropped image, etc.), we're
		// using a hack: we're using the "metadata" row key to insert meta data, the "image_index"
		// row key to store the number of images and "imageX" row key to insert images
		// * metadata (SuperColumn)
		//   - object name (Column)
		//   - object description (Column)
		// * image_index (SuperColumn)
		//   - first available id for imageX (Column)
		// * images containing object, in key imageX (Super Column)
		//   - image hash / id (Column)
		//   - cropped image contents (Column)
		//   - distance to viewer (Column)
		//   - angle to viewer (Column)
		//   - inferred GPS position (Column)
		// The row key is the object hash / id / name.
		for (int object = 0; object < image.getDetectedObjectsCount(); ++object) {
			DetectedObject detected_object = image.getDetectedObjects(object);
			String object_id = detected_object.getName();
			int first_unused_image_id;
			boolean object_exists = detectedObjectExists(object_id);
			if (!object_exists) {
				// If the object doesn't exist, then we should also add the medatada supercolumn
				first_unused_image_id = 0;
				ArrayList<HColumn<String, String>> metadata_values =
						new ArrayList<HColumn<String,String>>();
				metadata_values.add(HFactory.createStringColumn("name", detected_object.getName()));
				metadata_values.add(HFactory.createStringColumn("description", ""));
				mutator.insert(object_id,
						Constants.CASSANDRA_OBJECT_TO_IMAGE_ASSOCIATIONS_COLUMN_FAMILY,
						HFactory.createSuperColumn(
								"metadata", metadata_values, string_serializer, string_serializer,
								string_serializer));
			} else {
				first_unused_image_id = 
						getDetectedObjectFirstAvailableImageId(detected_object.getName());
				
			}
			// Add the image super column
			// TODO(alex, andrei): add support for using multiple serializers for column values
			ArrayList image_values = new ArrayList();
			// Add image_hash = String;
			image_values.add(HFactory.createStringColumn("image_hash", image_hash));
			// Add cropped_image_contents = ByteArray;
			image_values.add(HFactory.createColumn("cropped_image_contents",
					ImageUtils.getCroppedImageContents(image.getImage(),
							detected_object).toByteArray(),
					string_serializer, bytearray_serializer));
			// Add distance_to_viewer = Integer;
			image_values.add(HFactory.createColumn("distance_to_viewer",
					detected_object.getDistanceToViewer(),
					string_serializer, integer_serializer));
			// Add angle = Integer;
			image_values.add(HFactory.createColumn("angle", detected_object.getAngleToViewer(),
					string_serializer, integer_serializer));
			// Add inferred_gps_position = String;
			image_values.add(HFactory.createStringColumn("inferred_gps_position",
					ImageUtils.inferGPSPosition(image.getLocalizationFeatures(), 
							detected_object).toString()));
			mutator.insert(object_id,
					Constants.CASSANDRA_OBJECT_TO_IMAGE_ASSOCIATIONS_COLUMN_FAMILY,
					HFactory.createSuperColumn(
							"image" + first_unused_image_id, image_values, string_serializer, 
							string_serializer, TypeInferringSerializer.get()));
			Logging.LOG(10, "Storing image <" + image_hash + "> under key: " + object_id +
					", super column: " + "image" + first_unused_image_id);
			// We change the value of the first available image id column
			ArrayList<HColumn<String, Integer>> image_index = 
					new ArrayList<HColumn<String,Integer>>();
			image_index.add(HFactory.createColumn("first_available_image_id",
					first_unused_image_id + 1, string_serializer, integer_serializer));
			mutator.insert(object_id,
					Constants.CASSANDRA_OBJECT_TO_IMAGE_ASSOCIATIONS_COLUMN_FAMILY,
					HFactory.createSuperColumn("image_index", image_index, 
							string_serializer, string_serializer, integer_serializer));
		}
		Logging.LOG(10, "Done storing image");
		return true;
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
	
	private int getDetectedObjectFirstAvailableImageId(String object_id) {
		SubColumnQuery<String, String, String, Integer> sub_column_query =
				HFactory.createSubColumnQuery(keyspace_operator, string_serializer,
						string_serializer, string_serializer, integer_serializer);
		sub_column_query
				.setColumnFamily(Constants.CASSANDRA_OBJECT_TO_IMAGE_ASSOCIATIONS_COLUMN_FAMILY)
				.setKey(object_id)
				.setSuperColumn("image_index")
				.setColumn("first_available_image_id");
        QueryResult<HColumn<String, Integer>> result = sub_column_query.execute();
        return result.get().getValue();
	}

	private boolean detectedObjectExists(String object_id) {
		SubColumnQuery<String, String, String, String> sub_column_query =
				HFactory.createSubColumnQuery(keyspace_operator, string_serializer,
						string_serializer, string_serializer, string_serializer);
		sub_column_query
				.setColumnFamily(Constants.CASSANDRA_OBJECT_TO_IMAGE_ASSOCIATIONS_COLUMN_FAMILY)
				.setKey(object_id)
				.setSuperColumn("image_index")
				.setColumn("first_available_image_id");
        QueryResult<HColumn<String, String>> result = sub_column_query.execute();
        return (result.get() != null);
	}

}
