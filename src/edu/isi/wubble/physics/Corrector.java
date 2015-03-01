package edu.isi.wubble.physics;

import java.util.Map;
import java.util.TreeMap;

import com.jme.math.Vector3f;
import com.jme.scene.Controller;
import com.jme.scene.Spatial;
import com.jmex.physics.DynamicPhysicsNode;


public class Corrector extends Controller {
	private static final long serialVersionUID = 1L;

	protected DynamicPhysicsNode _node;
	protected Spatial _orienting;
	
	protected Vector3f _desiredPos;
	protected float _lastDistance;
	
	protected TreeMap<String,MoveNotifyI> _handlers;
	protected TreeMap<String,Boolean> _handlerSend;
	
	public Corrector(DynamicPhysicsNode node, Spatial orienting) {
		this(node,orienting,5.0f);
	}
	
	public Corrector(DynamicPhysicsNode node, Spatial orienting, float speed) {
		_node = node;
		_orienting = orienting;
		setSpeed(speed);

		_handlers = new TreeMap<String,MoveNotifyI>();
		_handlerSend = new TreeMap<String,Boolean>();
		
		setActive(false);
	}
	
	/**
	 * set the position of this corrector
	 * don't forget to enable it if you want it
	 * to function.
	 * @param desired
	 */
	public void setPosition(Vector3f desired) {
		_desiredPos = desired;
		_lastDistance = _desiredPos.subtract(_node.getLocalTranslation()).length();
	}
	
	/**
	 * add a notification callback for when this corrector 
	 * turns itself off.
	 * @param o
	 */
	public void registerInterest(MoveNotifyI o, boolean sendResponse) {
		_handlers.put(o.getName(), o);
		_handlerSend.put(o.getName(), sendResponse);
	}
	
	/**
	 * remove our interest in this corrector.
	 * @param o
	 */
	public void removeInterest(MoveNotifyI o) {
		_handlers.remove(o.getName());
	}
	
	/**
	 * when we are active we need to move towards the 
	 * desired position unless we have achieved our goal.
	 * @param tpf
	 */
	public void update(float tpf) {
		if (_node.isResting()) 
			_node.unrest();
		
		Vector3f dir = _desiredPos.subtract(_node.getLocalTranslation());
		float distance = dir.length();
		if (distance < 0.1 || distance > _lastDistance) {
			_node.setLinearVelocity(new Vector3f(0,0,0));
			for (MoveNotifyI handler : _handlers.values()) 
				handler.moveFinished(this, true);
			setActive(false);
			return;
		}
		
		_lastDistance = distance;
		_node.setLinearVelocity(dir.normalize().mult(getSpeed()));
		
		dir.setY(0);
		dir.normalizeLocal();
		_orienting.lookAt(_orienting.getLocalTranslation().add(dir), Vector3f.UNIT_Y);
	}
}
