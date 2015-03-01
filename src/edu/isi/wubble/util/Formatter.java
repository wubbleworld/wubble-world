package edu.isi.wubble.util;

import com.jme.math.Quaternion;
import com.jme.math.Vector3f;

public class Formatter {

	public static String format(Object obj) {
		if (obj == null) {
			return "null";
		}
		
		// we have to do this since java doesn't have multiple
		// dispatch.  Based on static time compilation of stored
		// objects java calls the method with the right parameters.
		if (obj instanceof Vector3f) {
			return format((Vector3f) obj);
		} else if (obj instanceof Quaternion) {
			return format((Quaternion) obj);
		}
		return obj.toString();
	}
	
	public static String format(Vector3f vec) {
		return vec.x + " " + vec.y + " " + vec.z;
	}
	
	public static String format(Quaternion quat) {
		return quat.x + " " + quat.y + " " + quat.z + " " + quat.w;
	}
}
