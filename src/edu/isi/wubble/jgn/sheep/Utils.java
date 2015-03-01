package edu.isi.wubble.jgn.sheep;

import static com.jme.math.FastMath.atan2;

import java.io.IOException;

import com.jme.bounding.OrientedBoundingBox;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.Spatial;
import com.jme.scene.state.AlphaState;
import com.jme.scene.state.CullState;
import com.jme.scene.state.MaterialState;
import com.jme.scene.state.RenderState;
import com.jme.system.DisplaySystem;
import com.jme.util.export.binary.BinaryImporter;
import com.jme.util.resource.ResourceLocatorTool;

import edu.isi.wubble.physics.entity.EntityManager;

public class Utils {
	public static ColorRGBA off = new ColorRGBA(0, 0, 0, 0);
	
	// Set the color of a Spatial.
	public static void setColor(Spatial spatial, ColorRGBA color) {
		final MaterialState materialState = DisplaySystem.getDisplaySystem().getRenderer().createMaterialState();
		materialState.setDiffuse(color);
		if (color.a < 1) {
			final AlphaState alphaState = DisplaySystem.getDisplaySystem().getRenderer()
					.createAlphaState();
			alphaState.setEnabled(true);
			alphaState.setBlendEnabled(true);
			alphaState.setSrcFunction(AlphaState.SB_SRC_ALPHA);
			alphaState.setDstFunction(AlphaState.DB_ONE_MINUS_SRC_ALPHA);
			spatial.setRenderState(alphaState);
			spatial.setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
		}
		spatial.setRenderState(materialState);
	}
	
	public static void makeTransparentZone(Spatial spatial, ColorRGBA color) {
		spatial.clearRenderState(RenderState.RS_MATERIAL);
		spatial.clearRenderState(RenderState.RS_ALPHA);
		
		MaterialState m = DisplaySystem.getDisplaySystem().getRenderer().createMaterialState();
		m.setDiffuse(color);
		m.setAmbient(color);
		m.setEmissive(off);
		m.setShininess(0);
		m.setSpecular(off);
		
		AlphaState a = DisplaySystem.getDisplaySystem().getRenderer().createAlphaState();
		a.setBlendEnabled(true);
		a.setTestEnabled(true);
//		a.setTestFunction(AlphaState.TF_GREATER);
		
		spatial.setRenderState(m);
		spatial.setRenderState(a);
		
		spatial.setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
		spatial.updateRenderState();
	}
	
	public static void makeTransparent(Spatial spatial, ColorRGBA color) {
		spatial.clearRenderState(RenderState.RS_MATERIAL);
		spatial.clearRenderState(RenderState.RS_ALPHA);
		
		MaterialState m = DisplaySystem.getDisplaySystem().getRenderer().createMaterialState();
		m.setDiffuse(color);
		m.setAmbient(off);
		m.setEmissive(off);
		m.setShininess(128);
		m.setSpecular(new ColorRGBA(0.8f, 0.8f, 0.8f, color.a));
		
		AlphaState a = DisplaySystem.getDisplaySystem().getRenderer().createAlphaState();
		a.setBlendEnabled(true);
		a.setTestEnabled(true);
		a.setTestFunction(AlphaState.TF_GREATER); // TF_NEVER is too strong, but greater better than equal, etc
		
		spatial.setRenderState(m);
		spatial.setRenderState(a);
		spatial.setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
		spatial.updateRenderState();
	}
	
	public static String vs(Vector3f v) { 
		return "(" + v.getX() + ", " + v.getY() + ", " + v.getZ() + ")"; 
	}
	
	// Load a model and attach it to the world.
	// ////////////////////////////////////////////////////////////
	public static Spatial loadModel(String filename) {
		Spatial retSpatial = null;
		try {
			retSpatial = (Spatial) BinaryImporter.getInstance().load(ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_MODEL, filename));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (retSpatial != null) {
			retSpatial.setModelBound(new OrientedBoundingBox());
			retSpatial.updateModelBound();
		}
		return retSpatial;
	}

//	public static WubbleGameState getWorldState() {
//		String targetState = "SheepPhysics";
//		
//		WubbleGameState wbs = (WubbleGameState) GameStateManager.getInstance().getChild(targetState);
//		if (wbs == null) {
//			System.out.println("BAD BAD BAD NULLLLL!!!!");
//		}
//
//		return wbs;
//	}

	// Convenience stuff.
	public static SheepPhysicsState GetSps() {
		return SheepPhysicsState.getWorldState();
	}
	
	public static EntityManager GetEM() {
		return GetSps().getEM();
	}
	
	public static Float[] vectorToFloats(Vector3f vec) {
		return new Float[]{vec.getX(), vec.getY(), vec.getZ()};
	}
	
	public static Float[] quaternionToFloats(Quaternion quat) {
		return new Float[]{quat.x, quat.y, quat.z, quat.w};
	}
	
	public static Quaternion floatsToQuaternion(Float[] f) {
		 return new Quaternion(f[0], f[1], f[2], f[3]);
	}
	
	public static Vector3f floatsToVector(Float[] f) {
		return new Vector3f(f[0], f[1], f[2]);
	}

	public static float extractXZAngle(Quaternion rot) {
		Vector3f ourDir = rot.getRotationColumn(2).normalize();
		return atan2(ourDir.z, ourDir.x);
	}
	
	// Hackery abounds!
	/////////////////////////////////////////////////////////////////	
	public static boolean FakeInstanceOf(String name, Object o) {
		boolean retVal = false;
		
		if (name.equals(o.getClass().getSimpleName())) { retVal = true; }
		return retVal;
	}

	public static final short IMPOSSIBLE_CLIENT_ID = -100;
}
