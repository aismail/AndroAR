package com.androar;

import java.awt.Container;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

public class OpenCVPanel extends JPanel {
	
	private void initPanel() {
		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		this.add(new JButton("OpenCVPanel"));
	}
	
	public OpenCVPanel() {
		super();
		initPanel();
	}
}
