package edu.isi.wubble.jgn.rpg;

import static com.jme.input.InputHandler.AXIS_NONE;

import com.jme.input.InputHandler;
import com.jme.input.action.InputAction;
import com.jme.input.action.InputActionEvent;
import com.jme.input.util.SyntheticButton;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.scene.Spatial;
import com.jmex.physics.DynamicPhysicsNode;
import com.jmex.physics.PhysicsSpace;
import com.jmex.physics.contact.ContactInfo;
import com.jmex.physics.geometry.PhysicsBox;
import com.jmex.physics.material.Material;

import edu.isi.wubble.physics.entity.ActiveEntity;
import edu.isi.wubble.physics.entity.DynamicEntity;
import edu.isi.wubble.physics.entity.EntityManager;

public class ModelEntity extends DynamicEntity {

	public ModelEntity(EntityManager em, String name) {
		super(em, name, true);
	}
	
	public static void createEntity(EntityManager em, Spatial s) {
		ModelEntity de = new ModelEntity(em, s.getName());
		
		DynamicPhysicsNode node = em.getPhysicsSpace().createDynamicNode();
		node.setMaterial(Material.WOOD);
		node.setLocalTranslation(s.getLocalTranslation());
		node.setLocalRotation(s.getLocalRotation());
		node.setName(s.getName());
		
		s.setLocalTranslation(new Vector3f());
		s.setLocalRotation(new Quaternion());
		
		node.attachChild(s);
		node.updateWorldVectors();

		if (s.getName().startsWith("barrel")) {
			PhysicsBox box = node.createBox("physicsBox");
			box.setLocalScale(new Vector3f(0.8f, 1, 0.8f));
			//physNode.generatePhysicsGeometry(false);
		} else if (s.getName().startsWith("coin")) {
			node.generatePhysicsGeometry(false);
		} else if (s.getName().startsWith("crate")) {
			PhysicsBox box = node.createBox("physicsBox");
			box.setLocalScale(new Vector3f(0.62f, 0.62f, 0.62f));
		}
		
		de.setNode(node);
		de.setPhysicsNode(node);
		de.setPickable(true);
	}
	
	/**
	 * create coins that are initially hidden from some wubbles
	 * this also generates the collision callback for the coin
	 * when the wubble hits it.
	 * @param s
	 */
	public static void createCoin(final RPGPhysics parent, final EntityManager em, Spatial s) {
		ModelEntity de = new ModelEntity(em, s.getName());
		DynamicPhysicsNode node = em.getPhysicsSpace().createDynamicNode();
		node.setMaterial(Material.WOOD);
		node.setLocalTranslation(s.getLocalTranslation());
		node.setLocalRotation(s.getLocalRotation());
		node.setName(s.getName());
		
		s.setLocalTranslation(new Vector3f());
		s.setLocalRotation(new Quaternion());
		
		node.attachChild(s);
		node.updateWorldVectors();
		node.generatePhysicsGeometry(false);

		de.setNode(node);
		de.setPhysicsNode(node);
		de.setPickable(true);
		em.addHiddenEntity(de);

		ActiveEntity picker = (ActiveEntity) em.getDynamicEntity("picker");
		final DynamicPhysicsNode pickNode = picker.getPhysicsNode();
		InputAction action = new InputAction() {
			protected boolean visible = false;
			
			public void performAction( InputActionEvent evt ) {
				final ContactInfo contactInfo = ( (ContactInfo) evt.getTriggerData() );

				if (!(contactInfo.getNode1() instanceof DynamicPhysicsNode) ||
					!(contactInfo.getNode2() instanceof DynamicPhysicsNode)) 
					return;
				
				if (visible) return;
				
				DynamicPhysicsNode node1 = (DynamicPhysicsNode) contactInfo.getNode1();
				DynamicPhysicsNode node2 = (DynamicPhysicsNode) contactInfo.getNode2();

				// the picker can now see this coin, because
				// they hit it.
				if (pickNode.equals(node1)) { 
					System.out.println("Now visible: " + node2.getName());
					DynamicEntity de = em.makeVisible(node2.getName());
					RPGServer.inst().sendToAll(parent.getClientIds(), de.generateSyncMessage());
					visible = true;
				} else if (pickNode.equals(node2)) {
					System.out.println("Now visible: " + node1.getName());
					DynamicEntity de = em.makeVisible(node1.getName());
					RPGServer.inst().sendToAll(parent.getClientIds(), de.generateSyncMessage());
					visible = true;
				}
			}
		};
		
		SyntheticButton eventHandler = node.getCollisionEventHandler();		
		em.getInputHandler().addAction(action, eventHandler.getDeviceName(), eventHandler.getIndex(), AXIS_NONE, false ); 
	}
	

}
