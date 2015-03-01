package edu.isi.wubble;

import java.io.IOException;

import com.jme.util.export.JMEExporter;
import com.jme.util.export.JMEImporter;
import com.jme.util.export.Savable;

public class JMEString implements Savable {
	
	private String text;
	
	public JMEString(String text) {
		this.text = text;
	}
	
	public String getString() {
		return text;
	}

	public Class getClassTag() {
		// TODO Auto-generated method stub
		return this.getClassTag();
	}

	public void read(JMEImporter arg0) throws IOException {
		// TODO Auto-generated method stub
		
	}

	public void write(JMEExporter arg0) throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	public String toString() {
		return text;
	}
}