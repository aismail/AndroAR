package com.androar;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.google.protobuf.Message;

public class Communication {
	public static byte[] readMessage(DataInputStream in) {
		byte[] message = null;
		try {
			int size = in.readInt();
			message = new byte[size];
			in.readFully(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return message;
	}
	
	public static void sendMessage(Message message, DataOutputStream out) {
		int size = message.getSerializedSize();
		try {
			out.writeInt(size);
			out.write(message.toByteArray());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
