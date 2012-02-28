package com.androar;

import java.io.*;
import java.net.Socket;

import com.androar.comm.Communication;
import com.androar.comm.CommunicationProtos.*;
import com.androar.comm.CommunicationProtos.ClientMessage.ClientMessageType;
import com.androar.comm.CommunicationProtos.ServerMessage.ServerMessageType;
import com.google.protobuf.InvalidProtocolBufferException;

public class ClientConnection implements Runnable {
	
	/*
	 * Creates a bridge between the requesting client and backend systems (i.e. Cassandra)
	 */
	public ClientConnection(Socket clientSocket) {
		this.clientSocket = clientSocket;
		try {
			out = new DataOutputStream(clientSocket.getOutputStream());
			in = new DataInputStream(clientSocket.getInputStream());
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
		Logging.LOG(0, "Client " + clientSocket.getInetAddress() + " connected.");
		
		// Just send a friendly hello
		ServerMessage helloMessage = 
				ClientConnection.createServerMessage(ServerMessageType.HELLO_MESSAGE);
		Logging.LOG(2, "Created hello message");
		Communication.sendMessage(helloMessage, out);
		
		// Parse incoming messages
		while (true) {
			try {
				byte[] serializedInputMessage = Communication.readMessage(in);
				if (serializedInputMessage == null) {
					break;
				}
				ClientMessage currentClientMessage = 
						ClientMessage.parseFrom(serializedInputMessage);
				ServerMessage replyMessage = 
						processAndReturnReplyToCurrentMessage(currentClientMessage);
				if (replyMessage != null) {
					Communication.sendMessage(replyMessage, out);
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
	private ServerMessage processAndReturnReplyToCurrentMessage(ClientMessage clientMessage) {
		ServerMessage.Builder builder = ServerMessage.newBuilder();
		ClientMessageType messageType = clientMessage.getMessageType();
		ServerMessage returnMessage = null;
		if (messageType == ClientMessageType.IMAGES_TO_STORE) {
			for (int image = 0; image < clientMessage.getImagesToStoreCount(); ++image) {
				// Right now we're just storing the image, assuming that it's relevant and that the
				// user input is correct.
				// TODO(alex, andrei): Fix.
				cassandra_connection.storeImage(clientMessage.getImagesToStore(image));
			}
		} else if (messageType == ClientMessageType.IMAGE_TO_PROCESS) {
			// Let's just store the image for now
			// TODO(alex): Fix.
			FileOutputStream fout;
			try {
				fout = new FileOutputStream("out.jpeg");
				fout.write(
						clientMessage
							.getImageToProcess()
							.getImage()
							.getImageContents()
							.toByteArray());
				fout.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			builder.setMessageType(ServerMessageType.IMAGE_PROCESSED);
			returnMessage = builder.build();
		}
		
		return returnMessage;
	}
	
	/*
	 * Creates a server message based on what the message type should be
	 * @param messageType the message type
	 */
	private static ServerMessage createServerMessage(ServerMessageType messageType) {
		ServerMessage.Builder builder = ServerMessage.newBuilder();
		
		if (messageType == ServerMessageType.HELLO_MESSAGE) {
			
		} else if (messageType == ServerMessageType.AUTHENTIFICATION_DENIED) {
			
		} else if (messageType == ServerMessageType.AUTHENTIFICATION_NEW_KEY) {
			
		} else if (messageType == ServerMessageType.IMAGE_PROCESSED) {
			
		}
		builder.setMessageType(messageType);
		return builder.build();
	}
	
	// Cassandra connection
	private CassandraDatabaseConnection cassandra_connection;
	// Socket between this server and the client
	private Socket clientSocket;
	// Output stream
	DataOutputStream out;
	// Input stream
	DataInputStream in;
}
