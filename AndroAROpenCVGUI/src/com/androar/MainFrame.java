package com.androar;

import java.awt.CardLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class MainFrame extends JFrame {

	private static String DATABASE_PANEL = "database_panel";
	private static String OPENCV_PANEL = "opencv_panel";
	// Main frame contains:
	// * panel with the buttons: browse_database, query_opencv
	// * card panel with the page we want
	JPanel buttons_pane = null;
	JPanel browse_database_panel = null;
	JPanel opencv_query_panel = null;
	JPanel cards_panel = null;

	Container default_container;

	JButton pressme = new JButton("Press Me");

	private void initPanels() {
		default_container = this.getContentPane();
		// Initialize panels
		buttons_pane = new JPanel();
		browse_database_panel = new DatabasePanel();
		opencv_query_panel = new OpenCVPanel();
		// Frame stats
		setBounds(0, 0, GUIConstants.WIDTH, GUIConstants.HEIGHT);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// Add the two panels to the container
		default_container.setLayout(new BoxLayout(default_container,
				BoxLayout.Y_AXIS));
		default_container.add(buttons_pane);

		cards_panel = new JPanel(new CardLayout());
		cards_panel.add(browse_database_panel, DATABASE_PANEL);
		cards_panel.add(opencv_query_panel, OPENCV_PANEL);
		default_container.add(cards_panel);

		initButtonsPanel();
	}
	

	private void initButtonsPanel() {
		JButton button;
		button = new JButton("Database queries");
		button.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				CardLayout cl = (CardLayout)(cards_panel.getLayout());
				cl.show(cards_panel, DATABASE_PANEL);
			}
		});
		buttons_pane.add(button);
		
		button = new JButton("OpenCV queries");
		button.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				CardLayout cl = (CardLayout)(cards_panel.getLayout());
				cl.show(cards_panel, OPENCV_PANEL);
			}
		});
		buttons_pane.add(button);
	}


	public MainFrame() {
		super("AndroAR Database parser");
		initPanels();
		setVisible(true);
	}
}
