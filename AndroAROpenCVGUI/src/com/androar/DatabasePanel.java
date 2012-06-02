package com.androar;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;

import com.androar.comm.ImageFeaturesProtos.ObjectMetadata;

@SuppressWarnings("serial")
public class DatabasePanel extends JPanel {

	private static String OBJECT_METADATA_PANEL = "Object Metadata";
	private static String IMAGES_CONTAINING_OBJECT_PANEL = "Images containing Object";

	private IDatabaseConnection db;

	private void initDatabase() {
		db = new CassandraDatabaseConnection(Constants.DATABASE_HOST,
				Constants.DATABASE_PORT);
	}

	private void initPanel() {
		initDatabase();
		JTabbedPane tabbed_pane = new JTabbedPane();
		/*
		 * OBJECTS_METADATA query
		 */
		{
			final JPanel main_panel = new JPanel();
			main_panel.setPreferredSize(
					new Dimension(GUIConstants.WIDTH - 50, GUIConstants.HEIGHT - 100));
			main_panel.setLayout(new BoxLayout(main_panel, BoxLayout.Y_AXIS));
			JPanel query_panel = new JPanel();
			query_panel.setPreferredSize(new Dimension(GUIConstants.WIDTH - 50, 50));
			final JPanel response_panel = new JPanel();
			response_panel.setPreferredSize(
					new Dimension(GUIConstants.WIDTH - 50, GUIConstants.HEIGHT - 150));
			main_panel.add(query_panel);
			main_panel.add(new JScrollPane(response_panel));
			// Input text area
			final JTextField objects_ids_tf = new JTextField(
					"Query objects, separated by (,)", 50);
			query_panel.add(objects_ids_tf);
			// Go button
			JButton go = new JButton("GO");
			query_panel.add(go);
			go.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					String[] all_objects = objects_ids_tf.getText().split(",");
					List<String> all_objects_list = new ArrayList<String>();
					for (String object : all_objects) {
						all_objects_list.add(object);
					}
					Map<String, ObjectMetadata> ret = db
							.getObjectsMetadata(all_objects_list);
					// Result table
					String[] cols = { "ID", "Name", "Description" };
					String[][] values = new String[ret.entrySet().size()][3];
					int i = 0;
					for (Entry<String, ObjectMetadata> entry : ret.entrySet()) {
						values[i][0] = entry.getKey();
						values[i][1] = (entry.getValue().hasName()) ? (entry
								.getValue().getName()) : "-";
						values[i][2] = (entry.getValue().hasDescription()) ? (entry
								.getValue().getDescription()) : "-";
						++i;
					}
					JTable table = new JTable(values, cols);
					response_panel.add(new JScrollPane(table));
					response_panel.revalidate();
				}
			});
			tabbed_pane.add(OBJECT_METADATA_PANEL, new JScrollPane(main_panel));
		}
		/*
		 * IMAGES CONTAINING OBJECTS query
		 */
		{
			final JPanel main_panel = new JPanel();
			main_panel.setPreferredSize(
					new Dimension(GUIConstants.WIDTH - 50, GUIConstants.HEIGHT - 100));
			main_panel.setLayout(new BoxLayout(main_panel, BoxLayout.Y_AXIS));
			JPanel query_panel = new JPanel();
			query_panel.setPreferredSize(new Dimension(GUIConstants.WIDTH - 50, 50));
			final JPanel response_panel = new JPanel();
			response_panel.setPreferredSize(
					new Dimension(GUIConstants.WIDTH - 50, GUIConstants.HEIGHT - 150));
			main_panel.add(query_panel);
			main_panel.add(new JScrollPane(response_panel));
			final JTextField object_id_tf = new JTextField("Object ID", 10);
			query_panel.add(object_id_tf);
			JButton go = new JButton("GO");
			query_panel.add(go);
			go.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					String object_id = object_id_tf.getText();
					final List<ImageWithObject> ret = db.getAllImagesContainingObject(object_id);
					String[] data = new String[ret.size()];
					for (int i = 0; i < ret.size(); ++i) {
						data[i] = "Image " + i;
					}
					final JComboBox results_list = new JComboBox(data);
					response_panel.add(results_list);

					final JPanel split_left_panel = new JPanel();
					final JPanel split_right_panel = new JPanel();
					final JSplitPane split_pane = new JSplitPane(
							JSplitPane.HORIZONTAL_SPLIT, split_left_panel, split_right_panel);
					response_panel.add(split_pane);
					if (results_list.getSelectedIndex() != -1) {
						int index = results_list.getSelectedIndex();
						byte[] big_image = ret.get(index).big_image.getImageContents().
								toByteArray();
						byte[] cropped_image = ret.get(index).cropped_image;
						split_left_panel.add(new JLabel(new ImageIcon(big_image)));
						split_right_panel.add(new JLabel(new ImageIcon(
								cropped_image)));
					}
					// Combo box action listener
					results_list.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent arg0) {
							int index = results_list.getSelectedIndex();
							if (index == -1) {
								return;
							}
							split_left_panel.removeAll();
							split_right_panel.removeAll();
							byte[] big_image = ret.get(index).big_image.getImageContents().
									toByteArray();
							byte[] cropped_image = ret.get(index).cropped_image;
							split_left_panel.add(new JLabel(new ImageIcon(big_image)));
							split_right_panel.add(new JLabel(new ImageIcon(
									cropped_image)));
							response_panel.revalidate();
						}
					});
					main_panel.revalidate();
				}
			});
			tabbed_pane.add(IMAGES_CONTAINING_OBJECT_PANEL,main_panel);
		}
		this.add(tabbed_pane);
	}

	public DatabasePanel() {
		super();
		initPanel();
	}
}
