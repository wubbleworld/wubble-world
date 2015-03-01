package edu.isi.wubble.physics.entity;

import java.util.ArrayList;
import java.util.Random;

import com.jme.math.Vector3f;
import com.jme.scene.Spatial;
import com.jme.scene.TriMesh;
import com.jme.scene.shape.Sphere;
import com.jme.scene.state.CullState;
import com.jme.system.DisplaySystem;

import edu.isi.wubble.JMEString;
import edu.isi.wubble.character.WubbleCharacter;

public class ClientWubbleEntity extends WubbleEntity {

	protected WubbleCharacter _visualCharacter;
	
	protected int _nearId;
	
	protected double _prob;
	protected Random _random;
	
	public ClientWubbleEntity(EntityManager em, String name, boolean makeUnique) {
		super(em, name, makeUnique);

		initWubbleEntity(new Vector3f(0,0,0));
		
		_nearId = -1;
		_random = new Random();
		_prob = 1.0/60.0;
	}

	public void createVisualNode() {
	    CullState cs = DisplaySystem.getDisplaySystem().getRenderer().createCullState();
	    cs.setCullMode(CullState.CS_BACK);
		
		_visualCharacter = new WubbleCharacter(getName());
	    _visualCharacter.setRenderState(cs);
	    _visualCharacter.updateRenderState();
	    _visualCharacter.setCullMode(Spatial.CULL_NEVER);
		_visualNode = _visualCharacter.getVisualNode();
	}
	
	public boolean pickUp(String name) {
		boolean result = super.pickUp(name);

		if (result)
			_visualCharacter.playAnimation("pickUp");
		return result;
	}
	
	public void putDown() {
		super.putDown();
		_visualCharacter.playAnimation("putDown");
		
		// need to determine an escape route... using ray casting
		PhysicsEntity de = _lifting.getCarriedEntity();
		Vector3f size = de.getSize();
		Vector3f exitDir = findExitDirection(size);
		
		moveTo(getPosition().add(exitDir), false);
	}
	
	protected void fillMeshes() {
		Vector3f size = getSize();
		Sphere s = new Sphere(getName(), getPosition(), 10, 10, size.x);
		_meshes = new ArrayList<TriMesh>();
		_meshes.add(s);
	}	
	
	public void think() {
//		Controller c = _controlMap.get(MoveToController.class.getName());
//		if (!c.isActive()) {
//			if (Math.random() < _prob) {
//				Vector3f ourPos = getPosition();
//				Vector3f pos = new Vector3f((float)Math.random()*12.0f, ourPos.y, (float)Math.random()*12.0f);
//				((MoveToController)c).applyController(pos);
//			}
//		}
//
//		LiftingController lc = (LiftingController) _controlMap.get(LiftingController.class.getName());
//		if (lc.isActive()) {
//			// with probability 1/150th drop the thing we are carrying (assuming 30 fps)
//			if (Math.random() < 0.006666) {
//				lc.release();
//			}
//		} else {
//			if (_nearSet.size() > 0 && Math.random() < 0.1) {
//				int index = _random.nextInt(_nearSet.size());
//				String chosen = _nearSet.get(index);
//				DynamicEntity de = _entityManager.getDynamicEntity(chosen);
//				while (de == null && _nearSet.size() > 1) {
//					_nearSet.remove(index);
//
//					index = _random.nextInt(_nearSet.size());
//					chosen = _nearSet.get(index);
//					de = _entityManager.getDynamicEntity(chosen);
//				}
//
//				if (de != null) {
//					System.out.println("Picking up: " + chosen);
//					Controller co = getController(LiftingController.class.getName());
//					((LiftingController) co).applyController(de);
//				}
//			}
//		}
//		
//		if (Math.random() < 0.01) {
//			AutoBinding.bindingMsg(getName(), "jump", true);
//		}
	}
	
	public static ClientWubbleEntity create(EntityManager em, String name) {
		ClientWubbleEntity c = new ClientWubbleEntity(em, name, true);
		
//		c.addProp("Color", new JMEString("yellow"));
//		c.addProp("GeometryType", new JMEString("sphere"));
//		c.addProp("Size", new Vector3f(0.25f, 0.25f, 0.25f));

		c.addMovementControls(false);
		c.setupCollisions(false);
		
		return c;
	}
}