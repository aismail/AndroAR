package com.androar;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import com.androar.comm.Communication;

@SuppressWarnings("serial")
public class OpenCVPanel extends JPanel {
	
	Socket socket;
	DataOutputStream out;
    DataInputStream in;
	
	private void initClient() {
		try {
			socket = new Socket(GUIConstants.SERVER_HOST, 6666);
			out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());
            
            // Read a message, assume it's hello
            Communication.readMessage(in);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void initPanel() {
		initClient();
		JTabbedPane tabbed_pane = new JTabbedPane();
		
		this.add(tabbed_pane);
	}
	
	public OpenCVPanel() {
		super();
		initPanel();
	}
}
