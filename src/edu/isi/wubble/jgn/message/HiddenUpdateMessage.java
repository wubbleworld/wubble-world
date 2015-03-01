package edu.isi.wubble.jgn.message;

import java.util.ArrayList;

import com.captiveimagination.jgn.message.Message;
import com.captiveimagination.jgn.message.type.PlayerMessage;

public class HiddenUpdateMessage extends Message implements PlayerMessage {
	private ArrayList<String> names;

	private ArrayList<Float> x;
	private ArrayList<Float> y;
	private ArrayList<Float> z;
	
	private ArrayList<Float> rotX;
	private ArrayList<Float> rotY;
	private ArrayList<Float> rotZ;
	private ArrayList<Float> rotW;
	
	public ArrayList<String> getNames() {
		return names;
	}
	public void setNames(ArrayList<String> names) {
		this.names = names;
	}
	public ArrayList<Float> getX() {
		return x;
	}
	public void setX(ArrayList<Float> x) {
		this.x = x;
	}
	public ArrayList<Float> getY() {
		return y;
	}
	public void setY(ArrayList<Float> y) {
		this.y = y;
	}
	public ArrayList<Float> getZ() {
		return z;
	}
	public void setZ(ArrayList<Float> z) {
		this.z = z;
	}
	public ArrayList<Float> getRotX() {
		return rotX;
	}
	public void setRotX(ArrayList<Float> rotX) {
		this.rotX = rotX;
	}
	public ArrayList<Float> getRotY() {
		return rotY;
	}
	public void setRotY(ArrayList<Float> rotY) {
		this.rotY = rotY;
	}
	public ArrayList<Float> getRotZ() {
		return rotZ;
	}
	public void setRotZ(ArrayList<Float> rotZ) {
		this.rotZ = rotZ;
	}
	public ArrayList<Float> getRotW() {
		return rotW;
	}
	public void setRotW(ArrayList<Float> rotW) {
		this.rotW = rotW;
	}
	
	
}
