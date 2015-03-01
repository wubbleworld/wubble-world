package edu.isi.wubble.physics.entity.controller;

import static com.jme.math.FastMath.RAD_TO_DEG;
import static com.jme.math.FastMath.abs;
import static com.jme.math.FastMath.atan2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.jme.bounding.BoundingSphere;
import com.jme.input.InputHandler;
import com.jme.input.action.InputAction;
import com.jme.input.action.InputActionEvent;
import com.jme.input.util.SyntheticButton;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.Spatial;
import com.jme.scene.shape.Sphere;
import com.jme.scene.state.AlphaState;
import com.jme.scene.state.CullState;
import com.jme.scene.state.MaterialState;
import com.jme.system.DisplaySystem;
import com.jmex.physics.DynamicPhysicsNode;
import com.jmex.physics.contact.ContactInfo;
import com.jmex.physics.material.Material;

import edu.isi.wubble.jgn.sheep.Utils;
import edu.isi.wubble.physics.entity.ActiveEntity;
import edu.isi.wubble.physics.entity.DynamicEntity;
import edu.isi.wubble.physics.entity.Entity;
import edu.isi.wubble.physics.entity.EntityManager;
import edu.isi.wubble.util.Globals;

/**
 * making the NearSphereController based on physics collisions.
 * @author wkerr
 *
 */
public class NearSphereController extends EntityController {
	private static final long serialVersionUID = 1L;
	
	public static final float ERROR = 0.0001f;
	
	protected EntityManager _entityManager;
	protected DynamicEntity _entity;
	
	protected DynamicPhysicsNode _complex;
	protected HashMap<String,ArrayList<ContactInfo>> _collisionMap;
	
	protected float _influenceScale;
	
	public NearSphereController(DynamicEntity e) {
		_entity = e;
		_entityManager = _entity.getManager();
		
		_collisionMap = new HashMap<String,ArrayList<ContactInfo>>();
		
		if (e instanceof ActiveEntity) {
			_influenceScale = 8;
		} else {
			_influenceScale = 4;
		}
		
		init();
	}
	
	public void init() {
		Vector3f size = _entity.getSize();
		float max = Math.max(size.x, Math.max(size.y, size.z));
		
		AlphaState as = DisplaySystem.getDisplaySystem().getRenderer().createAlphaState();
		as.setBlendEnabled(true);
		as.setTestEnabled(true);
		as.setTestFunction(AlphaState.TF_GREATER);

		MaterialState ms = DisplaySystem.getDisplaySystem().getRenderer().createMaterialState();
		ms.setDiffuse(new ColorRGBA(1.0f,1.0f,1.0f,0.1f));
		ms.setAmbient(new ColorRGBA(0,0,0,0));
		ms.setEmissive(new ColorRGBA(0,0,0,0));
		ms.setSpecular(new ColorRGBA(0,0,0,0));
		ms.setShininess(0);
		
        CullState cs = DisplaySystem.getDisplaySystem().getRenderer().createCullState();
        cs.setCullMode(CullState.CS_BACK);
		
		Sphere s = new Sphere(_entity.getName() + "_ns", 16, 16, _influenceScale*max);
		s.setModelBound(new BoundingSphere());
		s.updateModelBound();
		s.setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
		s.setCullMode(Spatial.CULL_INHERIT);
		s.setRenderState(as);
		s.setRenderState(ms);
		s.setRenderState(cs);
		s.updateRenderState();		

		_complex = _entityManager.getPhysicsSpace().createDynamicNode();
		_complex.setName(_entity.getName() + "_ns");
		_complex.attachChild(s);
		_complex.generatePhysicsGeometry();
		_complex.setMaterial(Material.GHOST);
		_complex.setAffectedByGravity(false);
		_complex.setLocalTranslation(new Vector3f(_entity.getPosition()));
		//_complex.setLocalTranslation(_entity.getPosition());
		
		addHandler();
		
		_entityManager.getRootNode().attachChild(_complex);
	}
	
	private void addHandler() {
		// this handler is called during the update cycle for our physics space.
		// at that time we will simply record the contacts for later processing
		// by this controller.
		SyntheticButton handler = _complex.getCollisionEventHandler();
		
		_entityManager.getInputHandler().addAction(new InputAction() {
			private HashSet<String> _reportedMap = new HashSet<String>();
			
			public void performAction(InputActionEvent evt) {
				final ContactInfo contactInfo = ((ContactInfo) evt.getTriggerData());
				// Node1 should always (I hope) be our complex.  Therefore we 
				// can ignore it for now.
				String name2 = contactInfo.getNode2().getName();
				Entity entity2 = _entityManager.getEntity(name2);
	
				if (entity2 == null) {
					// this occurs when we are colliding with another near sphere or
					// when we collide with something that isn't strictly an Entity
//					if (!_reportedMap.contains(_complex.getName() + "," + name2)) {
//						_reportedMap.add(_complex.getName() + "," + name2);
//						System.out.println("ERROR - collision handler: " + _complex.getName() + " " + name2);
//					}
					return;
				}
				
				ArrayList<ContactInfo> list = _collisionMap.get(entity2.getName());
				if (list == null) {
					list = new ArrayList<ContactInfo>();
					_collisionMap.put(entity2.getName(), list);
				}
				list.add(contactInfo);
			}
		}, handler.getDeviceName(), handler.getIndex(), InputHandler.AXIS_NONE, false);
	}
	
	public void drawNearSphere(boolean b) { 
		if (b == false) { 
			_complex.setCullMode(Spatial.CULL_ALWAYS); 
		} else { 
			_complex.setCullMode(Spatial.CULL_INHERIT); 
		}
			
		_complex.updateRenderState();		
	}	
	
	@Override
	public void cleanup() {
		super.cleanup();
		if (_complex != null) {
			_entityManager.getRootNode().detachChild(_complex);
			_complex.detachAllChildren();
		}
	}
	

	public void update(float time) {
		if (!canUpdate())
			return;

		for (Map.Entry<String,ArrayList<ContactInfo>> entry : _collisionMap.entrySet()) {
			Entity e = _entityManager.getEntity(entry.getKey());
			_entity.recordAuto(e, "Near", true);
			_entity.addNearEntity(e.getName());

			if (Globals.IN_SHEEP_GAME) {
				ArrayList<ContactInfo> list = entry.getValue();
				for (ContactInfo ci : list) {
					predCheck(e, ci);
				}
			}
		}
		
		// Reset these after each tick.
		Vector3f v = _entity.getPosition();
		_complex.setLocalTranslation(v.x, v.y, v.z);
		// System.out.println("NS update: setting ns to entity's position: " + Utils.vs(_entity.getPosition()));
		_collisionMap.clear();
	}
	
	protected void predCheck(Entity e1, ContactInfo contact) {
		// near works on different sized influence areas
		// so smaller (non-autonomous) objects don't actually
		// have much influence as opposed to an autonomous 
		// bigger object.
		
		Vector3f ourDir = _entity.getRotation().getRotationColumn(2).normalize();
		Vector3f direction = contact.getContactPosition(null).subtract(_entity.getPosition()).normalize();
//		Vector3f direction = e1.getPosition().subtract(_entity.getPosition()).normalize();
		
		float xyRel  = RAD_TO_DEG * atan2(direction.y, direction.x);
		
		if (xyRel < 0) { 
			xyRel += 360.0f; 
		}
//		float dis = abs(e1.getPosition().y - _entity.getPosition().y) + ERROR;
		float dis = abs(contact.getContactPosition(null).y - _entity.getPosition().y) + ERROR;
		float delta = (_entity.getSize().y / 2.0f) + (e1.getSize().y / 2.0f);
		
//		_entity.recordSample(e1, "DistanceY", dis);
		_entity.recordSample(e1, "UpAngle", xyRel);
		
		// test for above (right angles and actually higher than us)
		if (xyRel > 225 && xyRel < 315) {
			if (dis >= delta) {
				_entity.recordAuto(e1, "Above", true);
			}
		}

		// test for below (does it have the right angles and is it actually lower)
		if (xyRel > 45 && xyRel < 135) {
			if (dis >= delta) {
				_entity.recordAuto(e1, "Below", true);
			}
		}

		float xzOur = RAD_TO_DEG * atan2(ourDir.z, ourDir.x);
		float xzRel = RAD_TO_DEG * atan2(direction.z, direction.x);
		
		float diff = xzRel - xzOur;
		if (diff < 0.0f) 
			diff += 360.0f;

		_entity.recordSample(e1, "OurXZAngle", xzOur);
		_entity.recordSample(e1, "RelXZAngle", xzRel);
		_entity.recordSample(e1, "DiffXZAngle", diff);
		
		if (diff < 45 || diff > 315) {
			_entity.recordAuto(e1, "InFrontOf", true);
		} else if (diff < 135) {
			_entity.recordAuto(e1, "RightOf", true);
		} else if (diff < 225) {
			_entity.recordAuto(e1, "InBackOf", true);
		} else {
			_entity.recordAuto(e1, "LeftOf", true);
		}
	}
}