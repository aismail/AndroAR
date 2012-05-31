package com.androar;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

public class DatabasePanel extends JPanel {
	
	private void initPanel() {
		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		this.add(new JButton("DatabasePanel"));
		this.add(new JButton("DatabasePanel"));
	}
	
	public DatabasePanel() {
		super();
		initPanel();
	}
}
