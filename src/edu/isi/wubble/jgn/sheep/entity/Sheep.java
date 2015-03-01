package edu.isi.wubble.jgn.sheep.entity;
import java.util.Hashtable;
import java.util.Set;

import com.jme.bounding.OrientedBoundingBox;
import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.scene.Spatial;
import com.jme.scene.shape.Cylinder;
import com.jmex.physics.DynamicPhysicsNode;
import com.jmex.physics.contact.ContactInfo;
import com.jmex.physics.material.Material;

import edu.isi.wubble.jgn.sheep.SheepPhysicsState;
import edu.isi.wubble.jgn.sheep.Utils;

public class Sheep extends SDynamicEntity {
	protected Cylinder _visCylinder;
	protected boolean  _isScored = false;
	public boolean isScored() { return _isScored; }
	public void    setScored() { _isScored = true; }
	
	private Quaternion _magicRotation;
	
	public Sheep(String name, int x, int y, int z) {
		
		// For now, no user controlling of sheep.
		super(name, Utils.IMPOSSIBLE_CLIENT_ID, new Vector3f(x, y, z));

		// Sheep use their own update strategy.
		// setUpdateStrategy(new SheepUpdateStrategy(this));
	}

	protected void makeBody() {
		DynamicPhysicsNode node = getNode();
		node.setMaterial(Material.GRANITE);
		
		_visCylinder = new Cylinder(node.getName(), 8, 8, 0.5f, 0.75f, true);
		node.attachChild(_visCylinder);
		
		
		_visCylinder.getLocalRotation().fromAngleAxis( FastMath.PI / 2, Vector3f.UNIT_X );

		_visCylinder.setModelBound(new OrientedBoundingBox());
		_visCylinder.updateModelBound();
		_visCylinder.updateGeometricState(0, true);
		
		_magicRotation = new Quaternion();
		_magicRotation.fromAngleNormalAxis(FastMath.nextRandomFloat() * FastMath.TWO_PI, new Vector3f(0,1,0));
	}
	
	
	@Override
	public void preUpdate() {
		super.preUpdate();
	}

	
	int updateCounter = 0;
	int updateVal = 3;
	public void postUpdate() {
		
		super.postUpdate();
		// If this Sheep isn't supposed to be behaving, return.
		if (isBehaviorActive() == false) { return; }
		
		// I don't want the sheep running around.
//		if (!SheepPhysicsState.PRODUCTION) { return; }
		
		float forceMultiplier = 50;
		
		// Throttle the rate of sheep updates.
		updateCounter++;
		if (updateCounter <= updateVal) { return; }
		updateCounter = 0;

		Quaternion rot = new Quaternion(_magicRotation);
		Vector3f direction = rot.getRotationColumn(2);
		
		direction.normalizeLocal();
		direction = direction.mult(forceMultiplier);
				
		Vector3f vel = new Vector3f();
		vel = getNode().getLinearVelocity(vel);
		
		float velMag = vel.length();

		// Cap velocities.
		if (velMag < 2 ) { getNode().addForce(direction); }

		
		//// 
		
		float mod1 = 10;
		
		float oldAngle = _magicRotation.toAngleAxis(new Vector3f());
		float newAngle = 0;
		
		float worldZ = getNode().getWorldTranslation().z;
		float worldX = getNode().getWorldTranslation().x;
		
		// Endzone avoidance.
		if (worldZ > 27) { newAngle = FastMath.DEG_TO_RAD * 179; }
		else if (worldZ < -27) { newAngle = FastMath.DEG_TO_RAD * 1; }
		else if (worldX < -19)  { newAngle = FastMath.DEG_TO_RAD * 90; }
		else if (worldX > 19) { newAngle = FastMath.DEG_TO_RAD * 270; }
		else {
			newAngle = oldAngle + FastMath.DEG_TO_RAD * (mod1 - ((2 * mod1) * FastMath.nextRandomFloat()));
			
			if (newAngle > FastMath.TWO_PI) {newAngle -= FastMath.TWO_PI;  } 
			else if (newAngle < 0)          { newAngle += FastMath.TWO_PI; }
		}
		
		// What's happening to keep the damn sheep upright? 
		_magicRotation.fromAngleAxis(newAngle, Vector3f.UNIT_Y);
		
		getNode().getLocalRotation().fromAngleNormalAxis(newAngle, Vector3f.UNIT_Y);
	}

	
	// Do I need this anymore?  It seems to work fine without it, and to not ruin my 
	// local input controls when I assign them.
	public Quaternion getRotation() {
		if (isBehaviorActive() == false) { return super.getRotation(); }
		else { return new Quaternion(_magicRotation); } 
	}

	
	@Override
	public void onCollision(Hashtable<String, ContactInfo> collisions) {
		super.onCollision(collisions);
		
		// Am I in an endzone?  If so, disappear.
		SheepPhysicsState w = SheepPhysicsState.getWorldState(); 
		Set<String> s = collisions.keySet();
		for (String eName : s) {
			SEntity e = SEntity.GetEntityForName(eName);

			// Sheep/Endzone behavior.
			// NOTE: This is where Lisp's methods would be good - does sheep/endzone collision handler
			// live in sheep, or endzone?  In an object-centric organization you have to make a decision,
			// which will be unsatisfying to somebody.
			if (e instanceof EndZone) {

				EndZone colZone = (EndZone)e;
				int team = colZone.getTeam();
				System.out.println("Sheep \"" + getName() + "\" collided with EZ \"" + colZone.getName() + "\"");

				// FIXME: Here there's the same issue as with Eater - scoring and removing should 
				// be atomic, and should be handled by GameMechanics.
				if (isScored() == false) {
					setScored();
					w.removeEntity(getName());
					w.getGameMechanics().scoreSheep(this, team);
				}
			}
		}
	}	

	public Spatial getVisual() { return _visCylinder; }
}
