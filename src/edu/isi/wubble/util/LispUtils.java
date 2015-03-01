package edu.isi.wubble.util;

import java.util.StringTokenizer;

import com.jme.math.Vector3f;

public class LispUtils {

	public static String toLisp(Vector3f vec) {
		return "(" + vec.x + " " + vec.y + " " + vec.z + ")";
	}
	
	public static Vector3f fromLisp(String msg) {
		Vector3f v = new Vector3f();
		StringTokenizer str = new StringTokenizer(msg, " ");
		v.setX(Float.parseFloat(str.nextToken()));
		v.setY(Float.parseFloat(str.nextToken()));
		v.setZ(Float.parseFloat(str.nextToken()));
		
		return v;
	}
}
