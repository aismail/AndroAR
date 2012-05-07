package com.androar.comm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.androar.comm.CommunicationProtos.AuthentificationInfo;
import com.androar.comm.CommunicationProtos.ClientMessage;
import com.androar.comm.CommunicationProtos.ClientMessage.ClientMessageType;
import com.androar.comm.ImageFeaturesProtos.DetectedObject;
import com.androar.comm.ImageFeaturesProtos.DetectedObject.DetectedObjectType;
import com.androar.comm.ImageFeaturesProtos.GPSPosition;
import com.androar.comm.ImageFeaturesProtos.Image;
import com.androar.comm.ImageFeaturesProtos.ImageContents;
import com.androar.comm.ImageFeaturesProtos.LocalizationFeatures;
import com.androar.comm.ImageFeaturesProtos.MultipleOpenCVFeatures;
import com.androar.comm.ImageFeaturesProtos.ObjectBoundingBox;
import com.androar.comm.ImageFeaturesProtos.ObjectMetadata;
import com.androar.comm.ImageFeaturesProtos.OpenCVFeatures;
import com.google.protobuf.ByteString;

public class Mocking {
	
	private static String image_hash = "IMAGE_HASH";
	private static List<String> object_ids = new ArrayList<String>();
	private static float latitude = 0;
	private static float longitude = 0;
	
	public static void setMetadata(String image_hash, List<String> object_ids,
			float latitude, float longitude) {
		Mocking.image_hash = image_hash;
		Mocking.object_ids = object_ids;
		Mocking.latitude = latitude;
		Mocking.longitude = longitude;
	}
	
	public static MultipleOpenCVFeatures createMockFeatures() {
		MultipleOpenCVFeatures.Builder builder = MultipleOpenCVFeatures.newBuilder();
		for (String object : object_ids) {
			OpenCVFeatures features = OpenCVFeatures.newBuilder()
				.setObjectId(object)
				.setKeypoints("KEYPOINTS_" + image_hash + "_" + object)
				.setFeatureDescriptor("FEATURE_DESCRIPTOR_" + image_hash + "_" + object)
				.build();
			builder.addFeatures(features);
		}
		OpenCVFeatures big_image_features = OpenCVFeatures.newBuilder()
				.setKeypoints("KEYPOINTS_" + image_hash)
				.setFeatureDescriptor("FEATURE_DESCRIPTOR_" + image_hash) 
				.build();
		builder.addFeatures(big_image_features);
		return builder.build();
	}
	
	
	public static Image createMockImage(byte[] content, ClientMessageType type) {
		ByteString image_contents_byte_string;
		if (content == null) {
			byte[] mock_contents = image_hash.getBytes();
			image_contents_byte_string = ByteString.copyFrom(mock_contents);
		} else {
			image_contents_byte_string = ByteString.copyFrom(content);
		}
		// Image contents
		ImageContents image_contents = ImageContents.newBuilder()
				.setImageHash(image_hash)
				.setImageContents(image_contents_byte_string)
				.build();

		Image.Builder image_builder = Image.newBuilder();
		image_builder.setImage(image_contents);

		if (type == ClientMessageType.IMAGES_TO_STORE) {
			// Detected objects
			for (String object_id : object_ids) {
				DetectedObject obj = DetectedObject.newBuilder()
						.setObjectType(DetectedObjectType.BUILDING)
						.setId(object_id)
						.setBoundingBox(
								ObjectBoundingBox.newBuilder().setTop(10).setBottom(100).setLeft(20)
								.setRight(50).build())
								.setAngleToViewer(15)
								.setMetadata(
										ObjectMetadata.newBuilder().setName("NAME_" + object_id)
										.setDescription("DESCRIPTION_" + object_id))
										.build();
				image_builder.addDetectedObjects(obj);
			}
		}
		// Localization features
		GPSPosition gps_position = GPSPosition.newBuilder().setLatitude(latitude)
				.setLongitude(longitude).build();
		image_builder.setLocalizationFeatures(
				LocalizationFeatures.newBuilder().setGpsPosition(gps_position).build());

		return image_builder.build();
	}
	
	public static ClientMessage createMockClientMessage(byte[] content, ClientMessageType type) {
		Image image = createMockImage(content, type);
        
        // Mock authentication info
        AuthentificationInfo auth_info = AuthentificationInfo.newBuilder()
        		.setPhoneId("PHONE_ID")
        		.setHash("CURRENT_HASH_OF_PHONE_ID")
        		.build();
        
        // Client message
        ClientMessage client_message;
        if (type == ClientMessageType.IMAGE_TO_PROCESS) {
        	client_message = ClientMessage.newBuilder()
                	.setAuthentificationInfo(auth_info)
                	.setMessageType(type)
                	.setImageToProcess(image)
                	.build();
        } else {
        	client_message = ClientMessage.newBuilder()
                	.setAuthentificationInfo(auth_info)
                	.setMessageType(type)
                	.addImagesToStore(image)
                	.build();
        }
        return client_message;
	}
	
	public static ClientMessage createMockClientMessage(String image_path, ClientMessageType type) 
			throws FileNotFoundException, IOException {
		File in_file = new File(image_path);
        FileInputStream fin = new FileInputStream(in_file);
        byte file_contents[] = new byte[(int) in_file.length()];
        fin.read(file_contents);
        
        return createMockClientMessage(file_contents, type);
	}
}
