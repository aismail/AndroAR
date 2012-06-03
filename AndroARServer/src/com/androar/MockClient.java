package com.androar;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.androar.comm.Communication;
import com.androar.comm.CommunicationProtos.ClientMessage.ClientMessageType;
import com.androar.comm.CommunicationProtos.ServerMessage;
import com.androar.comm.ImageFeaturesProtos.Image;
import com.androar.comm.Mocking;

public class MockClient {
	
	public static void main(String[] args) {
		Socket socket;
		DataOutputStream out;
        DataInputStream in;
        
        if (args[0] == "--help" || args[0] == "-h" || (args.length % 2 != 0)) {
        	System.out.println(
        			"Usage: \n" +
        			"\t[QUERY]\tjava -jar mock_client.jar server_hostname query_image\n" +
        			"\t[STORE]\tjava -jar mock_client.jar server_hostname query_image " + 
        			"object1_name object1_cropped_image ... objectN_name objectN_cropped_image");
        	return;
        }
        
		try {
			socket = new Socket(args[0], 6666);
			out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());
            
            // Read a message
            ServerMessage server_message = ServerMessage.parseFrom(Communication.readMessage(in));
            Logging.LOG(Logging.DEBUGGING, "***\n " + server_message.toString() + "\n***");
            
            // Assume that the message was a HELLO. Let's now send an image to see if this works.
            // We will read an image stored on the Hard Drive for now, it's path is being passed 
            // through params
            List<String> object_ids = new ArrayList<String>();
            List<byte[]> object_cropped_images = new ArrayList<byte[]>();

            for (int i = 1; i < args.length / 2; ++i) {
            	object_ids.add(args[2 * i]);
            	File in_file = new File(args[2 * i + 1]);
                FileInputStream fin = new FileInputStream(in_file);
                byte file_contents[] = new byte[(int) in_file.length()];
                fin.read(file_contents);
            	object_cropped_images.add(file_contents);
            }
            Mocking.setMetadata(Double.toString(Math.random()), object_ids, 45, 60);
            Mocking.object_cropped_images = object_cropped_images;
            ClientMessageType type = 
            		(args.length == 2) ? ClientMessageType.IMAGE_TO_PROCESS : 
            			ClientMessageType.IMAGES_TO_STORE;
            Communication.sendMessage(Mocking.createMockClientMessage(args[1], type), out);
            if (type == ClientMessageType.IMAGE_TO_PROCESS) {
            	Image returned_image = Image.parseFrom(Communication.readMessage(in));
            	Logging.LOG(Logging.CRITICAL, returned_image.toString());
            }
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