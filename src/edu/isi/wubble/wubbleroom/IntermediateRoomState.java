package edu.isi.wubble.wubbleroom;

import static com.jme.input.InputHandler.AXIS_NONE;
import static com.jme.util.resource.ResourceLocatorTool.TYPE_MODEL;
import static edu.isi.wubble.wubbleroom.ObjectEntity.SIZES;

import java.net.URL;
import java.util.Random;

import com.jme.bounding.BoundingSphere;
import com.jme.input.action.InputAction;
import com.jme.input.action.InputActionEvent;
import com.jme.input.util.SyntheticButton;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Node;
import com.jme.scene.state.CullState;
import com.jme.system.DisplaySystem;
import com.jme.util.export.binary.BinaryImporter;
import com.jme.util.resource.ResourceLocatorTool;
import com.jmex.physics.PhysicsNode;
import com.jmex.physics.StaticPhysicsNode;
import com.jmex.physics.contact.ContactInfo;

import edu.isi.wubble.JMEString;
import edu.isi.wubble.physics.entity.Entity;
import edu.isi.wubble.util.LispUtils;

public class IntermediateRoomState extends RoomHandlerState {

	protected Vector3f _wubbleStartPos;
	
	public IntermediateRoomState() {
		super();
	}
	
	protected void initInput() {
		super.initInput();
	}
	
	protected void initRoomObjects() {
		Random r = new Random();
		createGroup("group1", r.nextInt(2), new Vector3f(1,0,3), 2, 1, new Vector3f(SIZES[0], SIZES[0], SIZES[0]));
		createGroup("group2", r.nextInt(2), new Vector3f(3,0,3), 2, 1, new Vector3f(SIZES[0], SIZES[1], SIZES[0]));
		
		createGroup("group3", r.nextInt(2), new Vector3f(6,0,3), 2, 1, new Vector3f(SIZES[0], SIZES[2], SIZES[0]));
		createGroup("group4", r.nextInt(2), new Vector3f(8,0,3), 2, 1, new Vector3f(SIZES[0], SIZES[3], SIZES[0]));
		
		createGroup("group5", r.nextInt(2), new Vector3f(1,0,10), 2, 1, new Vector3f(SIZES[1], SIZES[2], SIZES[1]));
		createGroup("group6", r.nextInt(2), new Vector3f(3.2f,0,10), 2, 1, new Vector3f(SIZES[1], SIZES[4], SIZES[1]));
		
		createGroup("group7", 2, new Vector3f(6,0,10), 2, 2, new Vector3f(SIZES[0], SIZES[0], SIZES[0]));
		
		Entity se = _entityManager.createStaticBox("platform", new Vector3f(1.5f, 2.2f, 3.0f));
		se.setColor(ColorRGBA.green);
		se.setPosition(new Vector3f(11.25f, 1.1f, 10.5f));
		se.getNode().setUserData("color", new JMEString("green"));
		
		_rootNode.attachChild(se.getNode());
		
		createApple();
	}
	
	protected void createApple() {
		try {
			URL url = ResourceLocatorTool.locateResource(TYPE_MODEL, "apple.jme");
			Node n = (Node) BinaryImporter.getInstance().load(url);
			n.setName("apple");
			n.setModelBound(new BoundingSphere());
			n.updateModelBound();
			n.setLocalTranslation(new Vector3f(11.25f, 2.3f, 10.5f));
			
			Entity se = _entityManager.createEntity(n, false, true);
			se.getNode().setUserData("geometry-type", new JMEString("sphere"));
			se.getNode().setUserData("size", new JMEString(LispUtils.toLisp(new Vector3f(0.1f, 0.1f, 0.1f))));
			se.getNode().setUserData("color", new JMEString("red"));
			
	        CullState cs = DisplaySystem.getDisplaySystem().getRenderer().createCullState();
	        cs.setCullMode(CullState.CS_BACK);
	        n.setRenderState(cs);
	        n.updateRenderState();

			_rootNode.attachChild(se.getNode());

			SyntheticButton eventHandler = ((StaticPhysicsNode) se.getNode()).getCollisionEventHandler();
			_input.addAction( new InputAction() {
				public void performAction( InputActionEvent evt ) {
					final ContactInfo contactInfo = ( (ContactInfo) evt.getTriggerData() );
					
					PhysicsNode node1 = contactInfo.getNode1();
					PhysicsNode node2 = contactInfo.getNode2();
					
					if ("jean".equals(node1.getName()) || "jean".equals(node2.getName())) {
						// the child wins!!!!!! we display a winning screen and kick them
						// back to the main room.
						RoomManager.getChat().systemMessage("You got the apple!! You win!!!");
					}
				}
			}, eventHandler.getDeviceName(), eventHandler.getIndex(), AXIS_NONE, false ); 
			
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	public void beginJump(String pos) {
		_wubbleStartPos = new Vector3f(_wubble.getPosition());
	}
	
	public void jump(String to) {
		Vector3f toVec = LispUtils.fromLisp(to);
		if (_wubbleStartPos == null) {
			_wubbleStartPos = new Vector3f(_wubble.getPosition());
		}
			
		if (toVec.y > (_wubbleStartPos.y + 0.75f)) {
			RoomManager.getChat().speak("It's too high!");
			SocketClient.inst().sendMessage("response move T");
			return;
		}

		super.jump(to);
	}
	
	@Override
	public float getPctDone() {
		// TODO Auto-generated method stub
		return 0;
	}
}
