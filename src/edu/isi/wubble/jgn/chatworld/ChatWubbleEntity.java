package edu.isi.wubble.jgn.chatworld;

import static edu.isi.wubble.jgn.message.InvokeMessage.createMsg;

import com.jme.bounding.BoundingBox;
import com.jme.math.Vector3f;
import com.jme.scene.Node;
import com.jme.scene.shape.Box;
import com.jmex.physics.DynamicPhysicsNode;

import edu.isi.wubble.jgn.message.InvokeMessage;
import edu.isi.wubble.physics.entity.EntityManager;
import edu.isi.wubble.physics.entity.WubbleEntity;

public class ChatWubbleEntity extends WubbleEntity {

	public ChatWubbleEntity(EntityManager em, String name) {
		super(em, name, false);
	}

	/**
	 * we are overriding the send of initial messages
	 * so that we can send create the wubble message
	 * rather than update the position messages as is
	 * typical.
	 */
	public void sendInitialMessages(Short id) {
		sendServerId(id);
		
		Object[] args = new Object[] {
				getName(), getPosition(), getRotation(), getColor()  
		};
		InvokeMessage msg = createMsg("addWubble", args);
		msg.sendTo(id);
	}
	
	public static ChatWubbleEntity createWubble(EntityManager em, String name, boolean local) {
		DynamicPhysicsNode dpn = em.getPhysicsSpace().createDynamicNode();
		dpn.setLocalTranslation(new Vector3f(0,5,0));
		dpn.setLocalScale(new Vector3f(0.25f, 0.25f, 0.25f));
		dpn.setName(name);
		dpn.createSphere(name);
		dpn.generatePhysicsGeometry();
		dpn.computeMass();
	
		Box b = new Box(name, new Vector3f(0,0,0), 0.5f, 0.5f, 0.5f);
		b.setModelBound(new BoundingBox());
		b.updateModelBound();
	
		Node visual = new Node(name);
		visual.attachChild(b);
		visual.setLocalScale(new Vector3f(0.25f, 0.25f, 0.25f));
		visual.lookAt(new Vector3f(0,0,-1), new Vector3f(0,1,0));
		
		Node parent = new Node(name);
		parent.attachChild(dpn);
		parent.attachChild(visual);
		
		ChatWubbleEntity we = new ChatWubbleEntity(em, name);
		we._physicsNode = dpn;
		we._visualNode = visual;
		we.setNode(parent);
		we.addMovementControls(!local);
		return we;
	}	

}

