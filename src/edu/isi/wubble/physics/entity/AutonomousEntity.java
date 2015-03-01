package edu.isi.wubble.physics.entity;

import com.jme.math.Vector3f;
import com.jme.scene.Controller;
import com.jmex.physics.DynamicPhysicsNode;
import com.jmex.physics.material.Material;

import edu.isi.wubble.AutoBinding;
import edu.isi.wubble.physics.entity.controller.LiftingController;
import edu.isi.wubble.physics.entity.controller.MoveToController;
import edu.isi.wubble.physics.entity.controller.UprightController;
import edu.isi.wubble.util.Globals;

public class AutonomousEntity extends DynamicEntity {
	
	protected boolean _think;
	
	public AutonomousEntity(EntityManager em, String name, boolean makeUnique) {
		this(em, name, makeUnique, false);
	}
	
	public AutonomousEntity(EntityManager em, String name, boolean makeUnique, boolean playerControlled) {
		super(em, name, makeUnique);
		
		createPhysicsNode();
		setNode(_physicsNode);
		setPhysicsNode(_physicsNode);
		
		_entityManager.getRootNode().attachChild(getNode());
		_entityManager.addUpdateEntity(this);
		
		addMovementControls(!playerControlled);

		UprightController uc = new UprightController(this);
		uc.setActive(true);
		addController(uc);
		
		_think = !playerControlled;
	}
	
	public void createPhysicsNode() {
		DynamicPhysicsNode dpn = _entityManager.getPhysicsSpace().createDynamicNode();
		dpn.setName(getName());
		dpn.attachChild(createBox(getName(), 1, 1, 1));
		dpn.setLocalTranslation(new Vector3f(0,0,0));
		dpn.setLocalScale(new Vector3f(0.50f,0.50f,0.50f));
		dpn.generatePhysicsGeometry(true);
		dpn.setMaterial(Material.ICE);
		dpn.computeMass();	
		
		_physicsNode = dpn;
	}
	
	public String getEntityType() {
		return "autonmous";
	}
	
	public void think() {
		if (!_think)
			return;
		
		Controller c = _controlMap.get(MoveToController.class.getName());
		if (!c.isActive()) {
			if (Math.random() < 0.016) {
				Vector3f ourPos = getPosition();
				Vector3f pos = new Vector3f((float)Math.random()*12.0f, ourPos.y, (float)Math.random()*12.0f);
				((MoveToController)c).applyController(pos);
			}
		}

		LiftingController lc = (LiftingController) _controlMap.get(LiftingController.class.getName());
		if (lc.isActive()) {
			// with probability 1/150th drop the thing we are carrying (assuming 30 fps)
			if (Math.random() < 0.001) {
				lc.release();
			}
		} else {
			if (_nearSet.size() > 0 && Math.random() < 0.007) {
				int index = Globals.random.nextInt(_nearSet.size());
				String chosen = _nearSet.get(index);
				DynamicEntity de = _entityManager.getDynamicEntity(chosen);
				while (de == null && _nearSet.size() > 1) {
					_nearSet.remove(index);

					index = Globals.random.nextInt(_nearSet.size());
					chosen = _nearSet.get(index);
					de = _entityManager.getDynamicEntity(chosen);
				}

				if (de != null) {
					System.out.println("Picking up: " + chosen);
					Controller co = getController(LiftingController.class.getName());
					((LiftingController) co).applyController(de);
				}
			}
		}
		
		if (Math.random() < 0.01) {
			AutoBinding.bindingMsg(getName(), "jump", true);
		}		
	}

}
