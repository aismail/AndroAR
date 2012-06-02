package com.androar;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import com.androar.comm.Communication;
import com.androar.comm.CommunicationProtos.ClientMessage;
import com.androar.comm.CommunicationProtos.ClientMessage.ClientMessageType;
import com.androar.comm.ImageFeaturesProtos.Image;
import com.androar.comm.ImageFeaturesProtos.PossibleObject;
import com.androar.comm.Mocking;

@SuppressWarnings("serial")
public class OpenCVPanel extends JPanel {

	private static String PROCESS_PANEL = "Process Image";

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

		/*
		 * PROCESS query
		 */
		{
			final JPanel main_panel = new JPanel();
			main_panel.setPreferredSize(
					new Dimension(GUIConstants.WIDTH - 50, GUIConstants.HEIGHT - 100));
			main_panel.setLayout(new BoxLayout(main_panel, BoxLayout.Y_AXIS));
			final JPanel query_panel = new JPanel();
			query_panel.setPreferredSize(new Dimension(GUIConstants.WIDTH - 50, 50));
			final JPanel response_panel = new JPanel();
			response_panel.setPreferredSize(
					new Dimension(GUIConstants.WIDTH - 50, GUIConstants.HEIGHT - 150));
			main_panel.add(query_panel);
			main_panel.add(new JScrollPane(response_panel));
			// imagine, hash = random, latitudine, longitudine, azimut
			final JFileChooser image_fc = new JFileChooser("..");
			final JTextField file_name_tf = new JTextField("No file", 10);
			file_name_tf.setEditable(false);
			final JButton choose_file_b = new JButton("Choose file");
			choose_file_b.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					image_fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
					int ret = image_fc.showOpenDialog(query_panel);
					if (ret == JFileChooser.APPROVE_OPTION) {
						File file = image_fc.getSelectedFile();
						file_name_tf.setText(file.getName());
					}
				}
			});
			final JTextField latitude_tf = new JTextField("Lat", 6);
			final JTextField longitude_tf = new JTextField("Lng", 6);
			final JTextField azimuth_tf = new JTextField("Azimuth", 6);
			query_panel.add(file_name_tf);
			query_panel.add(choose_file_b);
			query_panel.add(latitude_tf);
			query_panel.add(longitude_tf);
			query_panel.add(azimuth_tf);
			// Go button
			JButton go = new JButton("GO");
			query_panel.add(go);
			go.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					float latitude = Float.parseFloat(latitude_tf.getText());
					float longitude = Float.parseFloat(longitude_tf.getText());
					//float azimuth = Float.parseFloat(azimuth_tf.getText());
					File file = image_fc.getSelectedFile();
					if (file == null) {
						return;
					}
					String filename = file.getAbsolutePath();
					Mocking.setMetadata(Double.toString(Math.random()), null, latitude, longitude);
					try {
						ClientMessage message = Mocking.createMockClientMessage(
								filename, ClientMessageType.IMAGE_TO_PROCESS);
						Communication.sendMessage(message, out);
						
						Image returned_image = Image.parseFrom(Communication.readMessage(in));
						
						Vector<String> data = new Vector<String>();
						final List<byte[]> results = new ArrayList<byte[]>();
						for (int i = 0; i < returned_image.getPossiblePresentObjectsCount(); ++i) {
							PossibleObject obj = returned_image.getPossiblePresentObjects(i);
							for (int j = 0; j < obj.getFeaturesCount(); ++j) {
								results.add(obj.getFeatures(j).getResultMatch().toByteArray());
								data.add(obj.getId() + " " + j);
							}
						}
						
						final JComboBox results_list = new JComboBox(data);
						response_panel.add(results_list);
						final JPanel image_panel = new JPanel();
						response_panel.add(image_panel);
						if (results_list.getSelectedIndex() != -1) {
							int index = results_list.getSelectedIndex();
							byte[] big_image = results.get(index);
							image_panel.add(new JLabel(new ImageIcon(big_image)));
							response_panel.revalidate();
						}
						// Combo box action listener
						results_list.addActionListener(new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent arg0) {
								int index = results_list.getSelectedIndex();
								if (index == -1) {
									return;
								}
								image_panel.removeAll();
								byte[] big_image = results.get(index);
								image_panel.add(new JLabel(new ImageIcon(big_image)));
								response_panel.revalidate();
							}
						});
						response_panel.revalidate();
						main_panel.revalidate();
						
						
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});

			tabbed_pane.add(PROCESS_PANEL, new JScrollPane(main_panel));
		}

		this.add(tabbed_pane);
	}

import java.awt.Container;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import com.androar.comm.Communication;
import com.androar.comm.CommunicationProtos.ServerMessage;

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
            ServerMessage server_message = ServerMessage.parseFrom(Communication.readMessage(in));
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
