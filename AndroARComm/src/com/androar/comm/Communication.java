package com.androar.comm;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

import com.google.protobuf.Message;

public class Communication {
	public static byte[] readMessage(DataInputStream in) {
		byte[] message = null;
		// TODO(alex): There's no way in java to know if the socket closed on the other side
		// We can only read something and see if it throws a java.io.EOFException
		try {
			int size = in.readInt();
			message = new byte[size];
			in.readFully(message);
		} catch (EOFException e) {
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return message;
	}
	
	public static void sendByteArrayMessage(byte[] message, DataOutputStream out) {
		int size = message.length;
		try {
			out.writeInt(size);
			out.write(message);
		} catch (SocketException e) {
			return;
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}
	
	public static void sendMessage(Message message, DataOutputStream out) {
		int size = message.getSerializedSize();
		try {
			out.writeInt(size);
			out.write(message.toByteArray());
		} catch (SocketException e) {
			return;
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}

	public static byte[] sendAndProcessRequestToOpenCV(byte[] request, Socket socket) {
		int size = request.length;
		byte[] ret = null;
		try {
			DataOutputStream out = new DataOutputStream(socket.getOutputStream());
			DataInputStream in = new DataInputStream(socket.getInputStream());
			
			out.writeInt(size);
			out.write(request);
			size = in.readInt();
			ret = new byte[size];
			in.readFully(ret);
		} catch (EOFException e) {
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return ret;
	}
}
