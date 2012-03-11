package com.androar;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;

import com.androar.comm.Communication;
import com.androar.comm.CommunicationProtos.ClientMessage;
import com.androar.comm.CommunicationProtos.ClientMessage.ClientMessageType;
import com.androar.comm.CommunicationProtos.ServerMessage;
import com.androar.comm.CommunicationProtos.ServerMessage.ServerMessageType;
import com.google.protobuf.InvalidProtocolBufferException;

public class ClientConnection implements Runnable {
	
	/*
	 * Creates a bridge between the requesting client and backend systems (i.e. Cassandra)
	 */
	public ClientConnection(Socket client_socket) {
		this.client_socket = client_socket;
		try {
			out = new DataOutputStream(client_socket.getOutputStream());
			in = new DataInputStream(client_socket.getInputStream());
		} catch (IOException e) {
			Logging.LOG(0, e.getMessage());
		}
		// Initialize a connection to the Cassandra Cluster.
		cassandra_connection = 
				new CassandraDatabaseConnection(Constants.DATABASE_HOST, Constants.DATABASE_PORT);
		
		this.run();
	}
	
	@Override
	public void run() {
		Logging.LOG(0, "Client " + client_socket.getInetAddress() + " connected.");
		
		// Just send a friendly hello
		ServerMessage hello_message = 
				ClientConnection.createServerMessage(ServerMessageType.HELLO_MESSAGE);
		Logging.LOG(2, "Created hello message");
		Communication.sendMessage(hello_message, out);
		
		// Parse incoming messages
		while (true) {
			try {
				byte[] serialized_input_message = Communication.readMessage(in);
				if (serialized_input_message == null) {
					break;
				}
				ClientMessage current_client_message = 
						ClientMessage.parseFrom(serialized_input_message);
				ServerMessage reply_message = 
						processAndReturnReplyToCurrentMessage(current_client_message);
				if (reply_message != null) {
					Communication.sendMessage(reply_message, out);
				}
			} catch (InvalidProtocolBufferException e) {
				e.printStackTrace();
			}
		}
		cassandra_connection.closeConnection();
	}
	
	/*
	 * Processes a message received from the client and returns the appropriate response that should
	 * be sent back.
	 * @param clientMessage message received from client
	 */
	private ServerMessage processAndReturnReplyToCurrentMessage(ClientMessage client_message) {
		ServerMessage.Builder builder = ServerMessage.newBuilder();
		ClientMessageType message_type = client_message.getMessageType();
		ServerMessage return_message = null;
		if (message_type == ClientMessageType.IMAGES_TO_STORE) {
			for (int image = 0; image < client_message.getImagesToStoreCount(); ++image) {
				// Right now we're just storing the image, assuming that it's relevant and that the
				// user input is correct.
				// TODO(alex, andrei): Fix.
				cassandra_connection.storeImage(client_message.getImagesToStore(image));
			}
		} else if (message_type == ClientMessageType.IMAGE_TO_PROCESS) {
			// Let's just store the image for now
			// TODO(alex): Fix.
			FileOutputStream fout;
			try {
				fout = new FileOutputStream("out.jpeg");
				fout.write(
						client_message
							.getImageToProcess()
							.getImage()
							.getImageContents()
							.toByteArray());
				fout.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			builder.setMessageType(ServerMessageType.IMAGE_PROCESSED);
			return_message = builder.build();
		}
		
		return return_message;
	}
	
	/*
	 * Creates a server message based on what the message type should be
	 * @param messageType the message type
	 */
	private static ServerMessage createServerMessage(ServerMessageType message_type) {
		ServerMessage.Builder builder = ServerMessage.newBuilder();
		
		if (message_type == ServerMessageType.HELLO_MESSAGE) {
			
		} else if (message_type == ServerMessageType.AUTHENTIFICATION_DENIED) {
			
		} else if (message_type == ServerMessageType.AUTHENTIFICATION_NEW_KEY) {
			
		} else if (message_type == ServerMessageType.IMAGE_PROCESSED) {
			
		}
		builder.setMessageType(message_type);
		return builder.build();
	}
	
	// Cassandra connection
	private CassandraDatabaseConnection cassandra_connection;
	// Socket between this server and the client
	private Socket client_socket;
	// Output stream
	DataOutputStream out;
	// Input stream
	DataInputStream in;
}
