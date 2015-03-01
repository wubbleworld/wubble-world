package edu.isi.wubble.physics.entity.controller;

import java.util.HashMap;
import java.util.Map;

import com.jme.bounding.OrientedBoundingBox;
import com.jme.intersection.BoundingCollisionResults;
import com.jme.intersection.CollisionData;
import com.jme.intersection.CollisionResults;
import com.jme.math.FastMath;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Geometry;
import com.jme.scene.Node;
import com.jme.scene.shape.Box;

import edu.isi.wubble.jgn.message.InvokeMessage;
import edu.isi.wubble.jgn.sheep.SheepPhysicsState;
import edu.isi.wubble.jgn.sheep.Utils;
import edu.isi.wubble.jgn.sheep.entity.SDynamicEntity;
import edu.isi.wubble.jgn.sheep.entity.SEntity;
import edu.isi.wubble.jgn.sheep.entity.Sidekick;
import edu.isi.wubble.jgn.sheep.entity.Wubble;
import edu.isi.wubble.physics.entity.Entity;
import edu.isi.wubble.physics.entity.EntityManager;
import edu.isi.wubble.util.Highlighter;

// TODO: we want to be able to get the most salient entity (or ordered list of salience entities)
// event if there are multiple entities at 50 - need to preserve local information

public class VisualSalienceController extends EntityController {
	private static final long serialVersionUID = 7203871613507857100L;

	// Now this will be per-wubble
	private HashMap<Entity,Float> _salienceMap = new HashMap<Entity,Float>();
	private HashMap<Entity,Float> _simpleSalienceMap = new HashMap<Entity, Float>();
	
	private Wubble _wubble;
	private Box _fovBox;
	private Node _fovNode;
	private EntityManager _entityManager;
	
	private CollisionResults _res;
	
	private Entity _pointed;
	
	public VisualSalienceController(Wubble w) {
		_wubble = w;
		_res = new BoundingCollisionResults();
		_entityManager = _wubble.getManager();
		
		makeFOVBox();
	}
	
	private void makeFOVBox() {
		_fovBox = new Box(_wubble.getName() + "_fov", new Vector3f(0,0,0), 5, 2, 10);

		_fovNode = new Node(_wubble.getName() + "_fovNode");
		_fovNode.attachChild(_fovBox);
		
		_entityManager.getRootNode().attachChild(_fovNode);
		
		_fovNode.setLocalRotation(_wubble.getRotation());
		_fovNode.getLocalTranslation().set(_wubble.getPosition());
		_fovBox.getLocalTranslation().addLocal(0, 0, 8);
		
		Utils.makeTransparent(_fovBox, new ColorRGBA(0, 1, 1, 0.4f));
		
		_fovNode.setModelBound(new OrientedBoundingBox());
		_fovBox.setModelBound(new OrientedBoundingBox());
	}
	
	public HashMap<Entity, Float> getSalientEntities() { 
		return _salienceMap; 
	}
	
	
	// Return how salient entity e is to _wubble.  If it's not at all salient, return 0.
	public float getSalience(Entity e) {
		float sal = 0.0f;
		if (_salienceMap.containsKey(e)) { sal = _salienceMap.get(e); }
		return sal;
	}
	
	// This is where we can decay over time
	@Override
	public void update(float time) {
		findSalientObjects();
		
		_fovNode.getLocalTranslation().set(_wubble.getPosition());
		_fovNode.setLocalRotation(_wubble.getRotation());
		
		Highlighter.update(time);
	}
	
	private void findSalientObjects() {
		_fovNode.calculateCollisions(_wubble.getManager().getRootNode(), _res);
		
		_simpleSalienceMap = new HashMap<Entity, Float>();
		
		HashMap<Entity,Float> newSalienceMap = new HashMap<Entity,Float>();
		
		for (int i = 0; i < _res.getNumber(); i++) {
			CollisionData cd = _res.getCollisionData(i);
			Geometry gTgt = cd.getTargetMesh();
			
			String tgtName = gTgt.getName();
			
			SEntity eTgt = SEntity.GetEntityForName(tgtName);
			
			if (eTgt != null && 
				eTgt != _wubble && 
				(eTgt instanceof SDynamicEntity)) {
				
				SDynamicEntity object = (SDynamicEntity) eTgt;
				
				// Indicate that the entity is relevant to the wubble 
				float newSalience = computeSalience(object);
				newSalienceMap.put(object, newSalience);
				_simpleSalienceMap.put(object, newSalience);
			}
		}
		
		// Combine the new and old salience maps
		for (Map.Entry<Entity,Float> e : _salienceMap.entrySet()) {
			if (newSalienceMap.containsKey(e.getKey())) {
				float oldSalience = e.getValue();
				float newSalience = newSalienceMap.get(e.getKey());
				
				// salience buildup capped at 50
				// NB: we are really doing ToM and Salience in one metric
				// might want to factor them out
				if (oldSalience + newSalience >= 50.0f) {
					newSalienceMap.put(e.getKey(), 50.0f);
					
					if (oldSalience < 50.0f) {
						addAttention(e.getKey());
					}
				} else {
					newSalienceMap.put(e.getKey(), newSalience + oldSalience);
				}
				
				_wubble.recordSample(e.getKey(), "Salience", newSalienceMap.get(e.getKey()));
			} else {
				// This is decay by 1 each tick, removing if less than 1 
				if (e.getValue() > 1.0f) {
					newSalienceMap.put(e.getKey(), e.getValue() - 1.0f);
					_wubble.recordSample(e.getKey(), "Salience", newSalienceMap.get(e.getKey()));
				} else {
					removeAttention(e.getKey());
				}
			}
		}
		
		_salienceMap = newSalienceMap;
		
		_res.clear();
		
		// printSalienceMap();
	}
	
	public void addAttention(Entity e) {
		if (SheepPhysicsState.PRODUCTION) 
			return;
		
		if (!e.getName().equals("little " + _wubble.getName())) {
			InvokeMessage im = new InvokeMessage();
			im.setMethodName("addAttention");
			im.setArguments(new Object[] {e.getName()});
			im.sendTo(_wubble.getID());
			
			Sidekick s = _wubble.getSidekick();
			if (s != null && !_salienceMap.containsKey(_pointed)) {
				s.getAI().notifyAttention(e);
			}
		}
	}
	
	public void removeAttention(Entity e) {
		if (SheepPhysicsState.PRODUCTION) 
			return;
		
		if (!e.getName().equals("little " + _wubble.getName())) {
			InvokeMessage im = new InvokeMessage();
			im.setMethodName("removeAttention");
			im.setArguments(new Object[] {e.getName()});
			im.sendTo(_wubble.getID());
		}
	}
	
	public  void printSalienceMap() {
		for (Map.Entry<Entity,Float> e : _salienceMap.entrySet()) { 
			System.out.println("    " + _wubble.getName() + " attends to " + e.getKey().getName() + " w/salience --> " + e.getValue());
		}
	}
	
//	private float getOldSalience(Entity what) {
//		if (_salienceMap.containsKey(what)) {
//			float oldSalience = _salienceMap.get(what);
//			return oldSalience;
//		}
//		
//		return 0.0f;
//	}
	
	public Entity getMostSalientEntity() {
		if (_simpleSalienceMap.isEmpty()) {
			return null;
		} else {
			if (_simpleSalienceMap.size() == 1) {
				return _simpleSalienceMap.keySet().iterator().next();
			} else {
				float maxSalience = -1;
				Entity result = null;
				
				for (Map.Entry<Entity, Float> e : _simpleSalienceMap.entrySet() ) {
					if (e.getValue() > maxSalience) {
						return e.getKey();
					}
				}
				
				return result;
			}
		}
	}
	
	private float computeSalience(SDynamicEntity what) {
		if (what == null)
			return 0; 
		
//		System.out.println(_wubble.getName() + " attending to " + what.getName());
		
		_wubble.recordAuto(what, "Salient", true);
		
		float xDist = what.getPosition().getX() - _wubble.getPosition().getX();
		float zDist = what.getPosition().getZ() - _wubble.getPosition().getZ();
		
		Vector3f offset = new Vector3f(xDist, 0, zDist);
		
		// The angle the wubble is looking in
		float baseAngle = Utils.extractXZAngle(_wubble.getRotation());
		// The angle between the wubble and the object
		float objAngle = FastMath.atan2(zDist, xDist);
		
		float diffAngle = FastMath.abs(baseAngle - objAngle);
		
		float farDist = FastMath.abs(offset.length() * FastMath.sin(diffAngle));
		float sideDist = FastMath.abs(offset.length() * FastMath.cos(diffAngle));
		
		// Normalize to [0,1]
		float salience = (30 - (farDist + sideDist)) / 30;
		
//		System.out.println("a = " + farDist + ", b = " + sideDist);
//		System.out.println("Salience of " + what.getName() + "= " + salience);
		
		return salience;
		
//		_wubble.recordSample(what, "Salience", salience);
	}

	public void doPoint(Entity e) {
		_pointed = e;
	}
	
	@Override
	public void cleanup() {
		super.cleanup();
		
		if (_fovNode != null) {
			_entityManager.getRootNode().detachChild(_fovNode);
		}
	}
	
}
