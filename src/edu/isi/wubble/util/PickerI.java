package edu.isi.wubble.util;

import com.jme.scene.Node;
import com.jme.scene.Spatial;

public interface PickerI {
	public boolean isClickable(String name);
	
	/**
	 * The spatial that the user clicked on or null if they
	 * didn't click on anything.
	 * @param s
	 */
	public void picked(Spatial s);
	
	public Node getPickNode();
}
