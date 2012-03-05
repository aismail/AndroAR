package com.androar;

import java.io.*;
import java.net.Socket;

import com.androar.comm.Communication;
import com.androar.comm.CommunicationProtos.AuthentificationInfo;
import com.androar.comm.CommunicationProtos.ClientMessage;
import com.androar.comm.CommunicationProtos.ServerMessage;
import com.androar.comm.CommunicationProtos.ClientMessage.ClientMessageType;
import com.androar.comm.ImageFeaturesProtos.DetectedObject;
import com.androar.comm.ImageFeaturesProtos.Image;
import com.androar.comm.ImageFeaturesProtos.ImageContents;
import com.androar.comm.ImageFeaturesProtos.ObjectBoundingBox;
import com.androar.comm.ImageFeaturesProtos.DetectedObject.DetectedObjectType;
import com.androar.comm.ImageFeaturesProtos.ObjectMetadata;
import com.google.protobuf.ByteString;

public class MockClient {
	
	public static ClientMessage createMockClientMessage(String image_path) 
			throws FileNotFoundException, IOException {
		File in_file = new File(image_path);
        FileInputStream fin = new FileInputStream(in_file);
        byte file_contents[] = new byte[(int) in_file.length()];
        fin.read(file_contents);
        
        ByteString image_contents = ByteString.copyFrom(file_contents);
        
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
        		setDistanceToViewer(20).
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
	
	public static void main(String[] args) {
		Socket socket;
		DataOutputStream out;
        DataInputStream in;
        
		try {
			socket = new Socket(args[0], 6666);
			out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());
            
            // Read a message
            ServerMessage server_message = ServerMessage.parseFrom(Communication.readMessage(in));
            Logging.LOG(2, "***\n " + server_message.toString() + "\n***");
            
            // Assume that the message was a HELLO. Let's now send an image to see if this works.
            // We will read an image stored on the Hard Drive for now, it's path is being passed 
            // through params
            
            
            Communication.sendMessage(createMockClientMessage(args[1]), out);
            
            socket.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
	}
}