package com.androar;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.androar.comm.Communication;
import com.androar.comm.CommunicationProtos.ClientMessage.ClientMessageType;
import com.androar.comm.CommunicationProtos.ServerMessage;
import com.androar.comm.Mocking;

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
            List<String> object_ids = new ArrayList<String>();
            object_ids.add("BLAH");
            object_ids.add("ASDF");
            Mocking.setMetadata("md5", object_ids, 45, 60);
            Communication.sendMessage(
            		Mocking.createMockClientMessage(args[1], ClientMessageType.IMAGES_TO_STORE),
            		out);
            
            // Sleep for a while to allow opencv to process stuff and send us data
            // Until the user presses enter
            new Scanner(System.in).nextLine();
            
            socket.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
	}
}