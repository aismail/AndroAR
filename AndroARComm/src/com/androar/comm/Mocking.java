package com.androar.comm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.androar.comm.CommunicationProtos.AuthentificationInfo;
import com.androar.comm.CommunicationProtos.ClientMessage;
import com.androar.comm.CommunicationProtos.ClientMessage.ClientMessageType;
import com.androar.comm.ImageFeaturesProtos.DetectedObject;
import com.androar.comm.ImageFeaturesProtos.Image;
import com.androar.comm.ImageFeaturesProtos.ImageContents;
import com.androar.comm.ImageFeaturesProtos.ObjectBoundingBox;
import com.androar.comm.ImageFeaturesProtos.ObjectMetadata;
import com.androar.comm.ImageFeaturesProtos.DetectedObject.DetectedObjectType;
import com.google.protobuf.ByteString;

public class Mocking {
	
	public static ClientMessage createMockClientMessage(byte[] content) {
		ByteString image_contents = ByteString.copyFrom(content);
        
        Image image = Image.newBuilder().
        	addDetectedObjects(
        		DetectedObject.newBuilder().
        		setObjectType(DetectedObjectType.BUILDING).
        		setId("666").
        		setBoundingBox(
        			ObjectBoundingBox.newBuilder().
        			setTop(0).
        			setBottom(100).
        			setLeft(0).
        			setRight(100).
        			build()).
        		setAngleToViewer(15).
        		setMetadata(ObjectMetadata.newBuilder().
        				setName("OBJECT_1").
        				setDescription("OBJECT_1_DESCRIPTION_LONG")).
        		build()).
        	setImage(
        		ImageContents.newBuilder().
        		setImageHash("IMAGE_HASH").
        		setImageContents(image_contents)).
        	build();
        
        ClientMessage client_message = ClientMessage.newBuilder()
        	.setAuthentificationInfo(
        		AuthentificationInfo.newBuilder()
        		.setPhoneId("PHONE_ID")
        		.setHash("CURRENT_HASH_OF_PHONE_ID")
        		.build())
        	.setMessageType(ClientMessageType.IMAGES_TO_STORE)
        	.addImagesToStore(image)
        	.build();
        
        return client_message;
	}
	
	public static ClientMessage createMockClientMessage(String image_path) 
			throws FileNotFoundException, IOException {
		File in_file = new File(image_path);
        FileInputStream fin = new FileInputStream(in_file);
        byte file_contents[] = new byte[(int) in_file.length()];
        fin.read(file_contents);
        
        return createMockClientMessage(file_contents);
	}
}
