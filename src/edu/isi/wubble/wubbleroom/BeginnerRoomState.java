package edu.isi.wubble.wubbleroom;

import static edu.isi.wubble.wubbleroom.ObjectEntity.SIZES;

import com.jme.math.Vector3f;

public class BeginnerRoomState extends RoomHandlerState {

	public BeginnerRoomState() {
		super();
	}
	
	protected void initInput() {
		super.initInput();
	}
	
	protected void initRoomObjects() {
		ObjectEntity oe = ObjectEntity.createDynamicBox(_entityManager, "box1", new Vector3f(SIZES[1], SIZES[1], SIZES[1]));
		oe.applyRandomColor();
		oe.setPosition(new Vector3f(4,2,4));
		_rootNode.attachChild(oe.getNode());
		
		oe = ObjectEntity.createSphere(_entityManager, "sphere1", SIZES[1]); 
		oe.applyRandomColor();
		oe.setPosition(new Vector3f(8,2,8));
		_rootNode.attachChild(oe.getNode());
		
		oe = ObjectEntity.createCone(_entityManager, "cone1", SIZES[1], SIZES[1]);
		oe.applyRandomColor();
		oe.setPosition(new Vector3f(8,2,4));
		_rootNode.attachChild(oe.getNode());
		
		oe = ObjectEntity.createCylinder(_entityManager, "cylinder1", SIZES[1], SIZES[1]);
		oe.applyRandomColor();
		oe.setPosition(new Vector3f(4,2,8));
		_rootNode.attachChild(oe.getNode());
	}
	
	@Override
	public float getPctDone() {
		// TODO Auto-generated method stub
		return 0;
	}
}
