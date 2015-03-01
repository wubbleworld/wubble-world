package edu.isi.wubble.wubbleroom;

import static edu.isi.wubble.wubbleroom.ObjectEntity.SIZES;

import java.util.Random;

import com.jme.math.Vector3f;

public class AdvancedRoomState extends RoomHandlerState {

	public AdvancedRoomState() {
		super();
	}
	
	protected void initInput() {
		super.initInput();
	}
	
	protected void initRoomObjects() {
		Random r = new Random();
		
		Vector3f origin = new Vector3f(6.0f, 0, 11.0f);
		float x = origin.x;

		float xLen = SIZES[1];
		float zLen = SIZES[1];
		
		int count = 0;
		for (int i = 0; i < 3; ++i) {
			float z = origin.z;
			for (int j = 0; j < 3; ++j) {
				ObjectEntity oe;
				float yLen = SIZES[r.nextInt(SIZES.length)];
				if (r.nextBoolean()) {
					// create a box
					oe = ObjectEntity.createDynamicBox(_entityManager, "object"+count, new Vector3f(xLen, yLen, zLen));
				} else {
					// create a cylinder
					oe = ObjectEntity.createCylinder(_entityManager, "object"+count, xLen, yLen);
				}
				
				float y = (yLen / 2.0f) + 1.0f;
				
				oe.applyRandomColor();
				oe.setPosition(new Vector3f(x,y,z));
				_rootNode.attachChild(oe.getNode());
				
				z -= 1.7f;
				++count;
			}
			x += 1.7f;
		}
		
		float height = SIZES[SIZES.length-1];
		ObjectEntity oe;
		if (r.nextBoolean()) {
			oe = ObjectEntity.createDynamicBox(_entityManager, "object"+count, new Vector3f(xLen,height,zLen));
		} else {
			oe = ObjectEntity.createCylinder(_entityManager, "object"+count, xLen, height);
		}
		oe.applyRandomColor();
		oe.setPosition(new Vector3f(1, ((height/2.0f) + 1.0f), 6));
		_rootNode.attachChild(oe.getNode());
	}
	
	@Override
	public float getPctDone() {
		// TODO Auto-generated method stub
		return 0;
	}
}
