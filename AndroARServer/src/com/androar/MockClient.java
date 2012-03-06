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
import com.androar.comm.Mocking;
import com.google.protobuf.ByteString;

public class MockClient {
	
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
            
            Communication.sendMessage(Mocking.createMockClientMessage(args[1]), out);
            
            socket.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
	}
}