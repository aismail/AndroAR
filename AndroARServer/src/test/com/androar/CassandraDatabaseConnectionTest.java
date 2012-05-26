package test.com.androar;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.androar.CassandraDatabaseConnection;
import com.androar.caching.ObjectFeatureCache;
import com.androar.comm.CommunicationProtos.ClientMessage.ClientMessageType;
import com.androar.comm.ImageFeaturesProtos.GPSPosition;
import com.androar.comm.ImageFeaturesProtos.Image;
import com.androar.comm.ImageFeaturesProtos.ImageContents;
import com.androar.comm.ImageFeaturesProtos.LocalizationFeatures;
import com.androar.comm.ImageFeaturesProtos.ObjectMetadata;
import com.androar.comm.ImageFeaturesProtos.OpenCVFeatures;
import com.androar.comm.Mocking;
import com.google.protobuf.ByteString;

public class CassandraDatabaseConnectionTest {
	
	private static boolean RUN_LOAD_TESTS = true;
	
	private static String HOSTNAME = "emerald";
	private static CassandraDatabaseConnection db = null;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		db = new CassandraDatabaseConnection(HOSTNAME, 9160, "AndroARTest");
		List<String> object_ids = new ArrayList<String>();
        object_ids.add("A");
        object_ids.add("B");
        Mocking.setMetadata("hash1", object_ids, 30, 30);
        db.storeImage(Mocking.createMockImage(null, ClientMessageType.IMAGES_TO_STORE));
        db.storeFeatures("hash1", Mocking.createMockFeatures());
        object_ids.clear();
        object_ids.add("C");
        Mocking.setMetadata("hash2", object_ids, 30, 31);
        db.storeImage(Mocking.createMockImage(null, ClientMessageType.IMAGES_TO_STORE));
        db.storeFeatures("hash2", Mocking.createMockFeatures());
        Mocking.setMetadata("hash3", object_ids, 29, 30);
        db.storeImage(Mocking.createMockImage(null, ClientMessageType.IMAGES_TO_STORE));
        db.storeFeatures("hash3", Mocking.createMockFeatures());
        object_ids.clear();
        object_ids.add("A");
        object_ids.add("C");
        Mocking.setMetadata("hash4", object_ids, 20, 30);
        db.storeImage(Mocking.createMockImage(null, ClientMessageType.IMAGES_TO_STORE));
        db.storeFeatures("hash4", Mocking.createMockFeatures());
        object_ids.clear();
        Mocking.setMetadata("hash5", object_ids, 20, 50);
        db.storeImage(Mocking.createMockImage(null, ClientMessageType.IMAGES_TO_STORE));
        db.storeFeatures("hash5", Mocking.createMockFeatures());
        Thread.sleep(3000);
	}
	
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		db.deleteTables();
	}
	
	@Test
	public void testGetObjectsMetadata() {
		List<String> object_ids = new ArrayList<String>();
		object_ids.clear();
        object_ids.add("A");
        object_ids.add("B");
        object_ids.add("C");
        Map<String, ObjectMetadata> ret = db.getObjectsMetadata(object_ids);
        Assert.assertEquals(object_ids.size(), ret.entrySet().size());
        for (Map.Entry<String, ObjectMetadata> entry : ret.entrySet()) {
        	String id = entry.getKey();
        	Assert.assertTrue(object_ids.contains(id));
        	ObjectMetadata expected_metadata = 
        			ObjectMetadata.newBuilder()
        				.setName("NAME_" + id)
        				.setDescription("DESCRIPTION_" + id).build();
        	Assert.assertArrayEquals(expected_metadata.toByteArray(), entry.getValue().toByteArray());
        }
	}

	@Test
	public void testGetObjectsMetadataNoObjects() {
		List<String> object_ids = new ArrayList<String>();
		object_ids.clear();
        object_ids.add("X");
        object_ids.add("Y");
        Map<String, ObjectMetadata> ret = db.getObjectsMetadata(object_ids);
        Assert.assertEquals(0, ret.entrySet().size());
	}

	
	@Test
	public void testGetObjectMetadata() {
		List<String> object_ids = new ArrayList<String>();
		object_ids.clear();
        object_ids.add("A");
        object_ids.add("B");
        object_ids.add("C");
        for (String id : object_ids) {
        	Assert.assertTrue(object_ids.contains(id));
        	ObjectMetadata expected_metadata = 
        			ObjectMetadata.newBuilder()
        				.setName("NAME_" + id)
        				.setDescription("DESCRIPTION_" + id).build();
        	Assert.assertArrayEquals(expected_metadata.toByteArray(),
        			db.getObjectMetadata(id).toByteArray());
        }
        Assert.assertNull(db.getObjectMetadata("X"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testGetRowKeysForImagesInRange() {
		LocalizationFeatures center = LocalizationFeatures.newBuilder()
				.setGpsPosition(
						GPSPosition.newBuilder().setLatitude(30).setLongitude(30).build())
				.build();
		try {
			Method method = CassandraDatabaseConnection.class.getDeclaredMethod(
					"getRowKeysForImagesInRange", LocalizationFeatures.class, double.class);
			method.setAccessible(true);
			// Range 2
			List<String> row_keys = (List<String>) method.invoke(db, center, 2);
			List<String> expected_row_keys = new ArrayList<String>();
			expected_row_keys.add("hash1");
			expected_row_keys.add("hash2");
			expected_row_keys.add("hash3");
			Assert.assertArrayEquals(expected_row_keys.toArray(), row_keys.toArray());
			// Range 10
			row_keys = (List<String>) method.invoke(db, center, 10);
			expected_row_keys.clear();
			expected_row_keys.add("hash1");
			expected_row_keys.add("hash2");
			expected_row_keys.add("hash3");
			expected_row_keys.add("hash4");
			Assert.assertArrayEquals(expected_row_keys.toArray(), row_keys.toArray());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testGetImagesInRange() {
		LocalizationFeatures center = LocalizationFeatures.newBuilder()
				.setGpsPosition(
						GPSPosition.newBuilder().setLatitude(30).setLongitude(30).build())
				.build();
		List<Image> expected_images_in_range = new ArrayList<Image>();
		List<String> expected_images_keys = new ArrayList<String>();
		// Range 2
		expected_images_keys.add("hash1");
		expected_images_keys.add("hash2");
		expected_images_keys.add("hash3");
		for (String hash : expected_images_keys) {
			expected_images_in_range.add(Image.newBuilder().setImage(
					ImageContents.newBuilder()
							.setImageHash(hash)
							.setImageContents(ByteString.copyFrom(hash.getBytes()))
							.build()).build());
		}
		List<Image> images_in_range = db.getImagesInRange(center, 2);
		Assert.assertEquals(images_in_range.size(), expected_images_in_range.size());
		for (Image image : images_in_range) {
			Assert.assertTrue(expected_images_in_range.contains(image));
		}
		// Range 10
		expected_images_keys.clear();
		expected_images_in_range.clear();
		expected_images_keys.add("hash1");
		expected_images_keys.add("hash2");
		expected_images_keys.add("hash3");
		expected_images_keys.add("hash4");
		for (String hash : expected_images_keys) {
			expected_images_in_range.add(Image.newBuilder().setImage(
					ImageContents.newBuilder()
							.setImageHash(hash)
							.setImageContents(ByteString.copyFrom(hash.getBytes()))
							.build()).build());
		}
		images_in_range = db.getImagesInRange(center, 10);
		Assert.assertEquals(images_in_range.size(), expected_images_in_range.size());
		for (Image image : images_in_range) {
			Assert.assertTrue(expected_images_in_range.contains(image));
		}
	}

	@Test
	public void testGetFeaturesForAllObjectsInRange() {
		LocalizationFeatures center = LocalizationFeatures.newBuilder()
				.setGpsPosition(
						GPSPosition.newBuilder().setLatitude(30).setLongitude(30).build())
				.build();
		Map<String, List<OpenCVFeatures>> expected_features =
				new HashMap<String, List<OpenCVFeatures>>();
		// Range 2
		// In range 2, we have images:
		// * hash1, containing objects A, B
		// * hash2, containing object C
		// * hash3, containing object C
		Map<String, List<String>> objects = new HashMap<String, List<String>>();
		List<String> a_images = new ArrayList<String>();
		a_images.add("hash1");
		objects.put("A", a_images);
		List<String> b_images = new ArrayList<String>();
		b_images.add("hash1");
		objects.put("B", b_images);
		List<String> c_images = new ArrayList<String>();
		c_images.add("hash2");
		c_images.add("hash3");
		objects.put("C", c_images);
		for (Map.Entry<String, List<String>> entry : objects.entrySet()) {
			List<OpenCVFeatures> object_features = new ArrayList<OpenCVFeatures>();
			String key = entry.getKey();
			List<String> image_hashes = entry.getValue();
			for (String image_hash : image_hashes) {
				object_features.add(OpenCVFeatures.newBuilder()
					.setObjectId(key)
					.setKeypoints("KEYPOINTS_" + image_hash + "_" + key)
					.setFeatureDescriptor("FEATURE_DESCRIPTOR_" + image_hash + "_" + key)
					.build());
			}
			expected_features.put(key, object_features);
		}
		Map<String, List<OpenCVFeatures>> features = db.getFeaturesForAllObjectsInRange(center, 2);
		Assert.assertEquals(expected_features.entrySet().size(), features.entrySet().size());
		for (Map.Entry<String, List<OpenCVFeatures>> entry : features.entrySet()) {
			String key = entry.getKey();
			List<OpenCVFeatures> value = entry.getValue();
			Assert.assertTrue(expected_features.containsKey(key));
			List<OpenCVFeatures> expected_value = expected_features.get(key);
			for (OpenCVFeatures returned_feature : value) {
				Assert.assertTrue(expected_value.contains(returned_feature));
			}
		}
	}

	@Test
	public void testGetAllImagesContainingObject() {
	}
	
	@Test
	public void testGetFeaturesForObject() {
	}
	
	@Test
	public void loadTestGetFeaturesForAllObjectsInRange() {
		if (RUN_LOAD_TESTS == false) {
			return;
		}
		LocalizationFeatures center = LocalizationFeatures.newBuilder()
				.setGpsPosition(
						GPSPosition.newBuilder().setLatitude(30).setLongitude(30).build())
				.build();
		for (int i = 0; i < 50; ++i) {
			long start_time = System.currentTimeMillis();
			db.getFeaturesForAllObjectsInRange(center, 2);
			long total_time = System.currentTimeMillis() - start_time;
			System.out.println(i + " : " + total_time);
		}
	}
	
	@Test
	public void testGetFeaturesForAllObjectsInRange_Caching() {
		LocalizationFeatures center = LocalizationFeatures.newBuilder()
				.setGpsPosition(
						GPSPosition.newBuilder().setLatitude(30).setLongitude(30).build())
				.build();
		Map<String, List<OpenCVFeatures>> expected_features =
				new HashMap<String, List<OpenCVFeatures>>();
		// Range 2
		// In range 2, we have images:
		// * hash1, containing objects A, B
		// * hash2, containing object C
		// * hash3, containing object C
		Map<String, List<String>> objects = new HashMap<String, List<String>>();
		List<String> a_images = new ArrayList<String>();
		a_images.add("hash1");
		objects.put("A", a_images);
		List<String> b_images = new ArrayList<String>();
		b_images.add("hash1");
		objects.put("B", b_images);
		List<String> c_images = new ArrayList<String>();
		c_images.add("hash2");
		c_images.add("hash3");
		objects.put("C", c_images);
		for (Map.Entry<String, List<String>> entry : objects.entrySet()) {
			List<OpenCVFeatures> object_features = new ArrayList<OpenCVFeatures>();
			String key = entry.getKey();
			List<String> image_hashes = entry.getValue();
			for (String image_hash : image_hashes) {
				object_features.add(OpenCVFeatures.newBuilder()
					.setObjectId(key)
					.setKeypoints("KEYPOINTS_" + image_hash + "_" + key)
					.setFeatureDescriptor("FEATURE_DESCRIPTOR_" + image_hash + "_" + key)
					.build());
			}
			expected_features.put(key, object_features);
		}
		ObjectFeatureCache.clearCache();
		for (int i = 0; i < 2; ++i) {
			Map<String, List<OpenCVFeatures>> features = db.getFeaturesForAllObjectsInRange(center, 2);
			Assert.assertEquals(expected_features.entrySet().size(), features.entrySet().size());
			for (Map.Entry<String, List<OpenCVFeatures>> entry : features.entrySet()) {
				String key = entry.getKey();
				List<OpenCVFeatures> value = entry.getValue();
				Assert.assertTrue(expected_features.containsKey(key));
				List<OpenCVFeatures> expected_value = expected_features.get(key);
				for (OpenCVFeatures returned_feature : value) {
					Assert.assertTrue(expected_value.contains(returned_feature));
				}
			}
		}
	}
}
