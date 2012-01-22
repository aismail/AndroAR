package com.androar;

import java.io.*;
import java.net.Socket;

import com.androar.CommunicationProtos.ClientMessage.ClientMessageType;
import com.androar.CommunicationProtos.ServerMessage.ServerMessageType;
import com.androar.CommunicationProtos.*;
import com.google.protobuf.InvalidProtocolBufferException;

public class ClientConnection implements Runnable {
	
	public ClientConnection(Socket clientSocket) {
		this.clientSocket = clientSocket;
		try {
			out = new DataOutputStream(clientSocket.getOutputStream());
			in = new DataInputStream(clientSocket.getInputStream());
		} catch (IOException e) {
			Logging.LOG(0, e.getMessage());
		}
		this.run();
	}
	
	@Override
	public void run() {
		Logging.LOG(0, "Client " + clientSocket.getInetAddress() + " connected.");
		
		// Just send a friendly hello
		ServerMessage helloMessage = ClientConnection.createServerMessage(ServerMessageType.HELLO_MESSAGE);
		Logging.LOG(2, "Created hello message");
		Communication.sendMessage(helloMessage, out);
		
		// Parse incoming messages
		while (true) {
			try {
				ClientMessage currentClientMessage = ClientMessage.parseFrom(Communication.readMessage(in));
				ServerMessage replyMessage = processAndReturnReplyToCurrentMessage(currentClientMessage);
				if (replyMessage != null) {
					Communication.sendMessage(replyMessage, out);
				}
			} catch (InvalidProtocolBufferException e) {
				e.printStackTrace();
			}
		}
	}
	private static ServerMessage processAndReturnReplyToCurrentMessage(ClientMessage client_message) {
		ServerMessage.Builder builder = ServerMessage.newBuilder();
		ClientMessageType messageType = client_message.getMessageType();
		if (messageType == ClientMessageType.IMAGE_TO_PROCESS) {
			
		} else if (messageType == ClientMessageType.IMAGE_TO_PROCESS) {
			// Let's just store the image for now
			// TODO(alex): Fix.
			FileOutputStream fout;
			try {
				fout = new FileOutputStream("out.jpeg");
				fout.write(client_message.getImageToProcess().getImage().getImageContents().toByteArray());
				fout.close();
			} catch (IOException e) {
				Logging.LOG(2, e.getMessage());
			}
			builder.setMessageType(ServerMessageType.IMAGE_PROCESSED);
		}
		
		return builder.build();
	}
	
	private static ServerMessage createServerMessage(ServerMessageType messageType) {
		ServerMessage.Builder builder = ServerMessage.newBuilder();
		
		// HELLO message
		if (messageType == ServerMessageType.HELLO_MESSAGE) {
			
		} else if (messageType == ServerMessageType.AUTHENTIFICATION_DENIED) {
			
		} else if (messageType == ServerMessageType.AUTHENTIFICATION_NEW_KEY) {
			
		} else if (messageType == ServerMessageType.IMAGE_PROCESSED) {
			
		}
		builder.setMessageType(messageType);
		return builder.build();
	}
	
	// Socket between this server and the client
	private Socket clientSocket;
	// Output stream
	DataOutputStream out;
	// Input stream
	DataInputStream in;
}
