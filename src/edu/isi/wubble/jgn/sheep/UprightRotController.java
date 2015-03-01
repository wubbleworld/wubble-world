package edu.isi.wubble.jgn.sheep;

import com.jme.input.controls.GameControl;
import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.scene.Controller;
import com.jme.scene.Spatial;

/**
 * @author Matthew D. Hicks
 */
public class UprightRotController extends Controller {
	private static final long serialVersionUID = -113610657294493721L;
	
	private Spatial spatial;
	private GameControl activate;
	
	private Quaternion quat;
	private Vector3f dir;
	
	public UprightRotController(Spatial spatial, GameControl activate) {
		this.spatial = spatial;
		this.activate = activate;
	}

	public void update(float time) {
		Vector3f notXDir = new Vector3f(0,1,1);
		if (activate.getValue() != 0) {
			Quaternion quat = spatial.getLocalRotation();
			//quat.fromAngleAxis(0, zDir);
			
			float[] angles = new float[3];
			quat.toAngles(angles);
			System.out.println("The angles are: (" + angles[0] + ", " + angles[1] + ", " + angles[2] + ")");
			
			quat.fromAngles(0, angles[1], angles[2]);
						
			//quat.multLocal(0,1,1,1);
			//spatial.getLocalRotation().multLocal(notXDir);
			System.out.println("Updating!");

			//quat.fromAngleAxis(delta * FastMath.PI, dir);
			//spatial.getLocalRotation().multLocal(quat);
		}
	}
}
