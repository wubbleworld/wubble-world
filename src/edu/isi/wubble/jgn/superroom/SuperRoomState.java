package edu.isi.wubble.jgn.superroom;

import static com.jme.input.InputHandler.AXIS_NONE;
import static com.jme.input.InputHandler.BUTTON_ALL;
import static com.jme.input.InputHandler.DEVICE_MOUSE;

import static com.jme.util.resource.ResourceLocatorTool.TYPE_MODEL;

import java.net.URL;
import java.util.HashMap;
import java.util.List;

import com.jme.input.ChaseCamera;
import com.jme.input.InputHandler;
import com.jme.input.KeyBindingManager;
import com.jme.input.KeyInput;
import com.jme.input.action.InputActionEvent;
import com.jme.input.action.KeyInputAction;
import com.jme.input.thirdperson.ThirdPersonMouseLook;
import com.jme.light.PointLight;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jme.scene.shape.Box;
import com.jme.scene.state.CullState;
import com.jme.scene.state.LightState;
import com.jme.scene.state.MaterialState;
import com.jme.system.DisplaySystem;
import com.jme.util.export.binary.BinaryImporter;
import com.jme.util.resource.ResourceLocatorTool;
import com.jmex.physics.StaticPhysicsNode;

import edu.isi.wubble.gamestates.PhysicsGameState;
import edu.isi.wubble.physics.TimeManager;
import edu.isi.wubble.physics.entity.AutoStaticEntity;
import edu.isi.wubble.physics.entity.AutonomousEntity;
import edu.isi.wubble.physics.entity.DynamicEntity;
import edu.isi.wubble.physics.entity.Entity;
import edu.isi.wubble.physics.entity.EntityManager;
import edu.isi.wubble.physics.entity.PhysicsEntity;
import edu.isi.wubble.physics.state.StateDatabase;
import edu.isi.wubble.physics.state.Tag;
import edu.isi.wubble.util.Globals;
import edu.isi.wubble.util.Highlighter;
import edu.isi.wubble.util.PickerI;
import edu.isi.wubble.util.PickerInputAction;
import edu.isi.wubble.util.Swift;

public class SuperRoomState extends PhysicsGameState implements PickerI {
	protected ChaseCamera      _chaseCamera;

	protected KeyInputAction   _switchAction;
	protected LightState       _lightState;
	
	protected StaticPhysicsNode  _roomNode;
//	protected ClientWubbleEntity _wubble;
	protected AutonomousEntity   _auto;
	
	protected boolean           _requestInput;
	protected EntityManager     _entityManager;
	
	public SuperRoomState() {
		super();

		init();
	}
	
	protected void init() {
		Globals.LOG_DYNAMIC = true;
		Globals.USE_DATABASE = true;
		Globals.SHANE_PRINTING = false;

		DisplaySystem.getDisplaySystem().getRenderer().getQueue().setTwoPassTransparency(false);
		
		_showBounding = false;
		_drawPhysics = false;
		_renderStatistics = true;
		_physics.setAutoRestThreshold(0.01f);
		_entityManager = new EntityManager(_physics, _input, _rootNode);

		// create a session now, because objects will be adding themselves
		// into the mix as they are created.  The entity_table will be populated
		// with objects and their names.
		StateDatabase.inst().createSession();
		StateDatabase.inst().beginTransaction();
		
		initLights();
		initRoom();
		initChaseCamera();
		
        _rootNode.updateWorldVectors();
		_rootNode.updateGeometricState(0, true);
		_rootNode.updateRenderState();
	
		_timer.reset();

		// now that all of the objects are created we can be guaranteed
		// that they all of have the correct meshes set up as well as
		// all the other good stuff.  We now set up the SWIFT package
		// used for tracking distances between objects.
		Swift.inst();
		
		for (Entity e : _entityManager.getAllEntities()) {
			e.addInitialProperties();
		}
		for (Entity e : _entityManager.getAllEntities()) {
			Swift.inst().addObject(e);
		}
		
		StateDatabase.inst().endTransaction();
	}
	
	protected void initRoom() {
//        _wubble = ClientWubbleEntity.create(_entityManager, "wubble");
//		_wubble.setPosition(new Vector3f(7.5f, 3.0f, 7.5f));
//        _rootNode.attachChild(_wubble.getNode());
        
        _auto = new AutonomousEntity(_entityManager, "auto", false, true);
        _auto.setPosition(new Vector3f(6.0f, 3.0f, 6.0f));
        
        new AutoStaticEntity(_entityManager, "floor", new Box("floor", new Vector3f(6,-0.1f,6), 6, 0.1f, 6), false);
        new AutoStaticEntity(_entityManager, "ceiling", new Box("ceiling", new Vector3f(6,12.1f,6), 6, 0.1f, 6), false);
        new AutoStaticEntity(_entityManager, "wall1", new Box("wall1", new Vector3f(-0.1f,6,6), 0.1f, 6, 6), false);
        new AutoStaticEntity(_entityManager, "wall2", new Box("wall2", new Vector3f(12.1f,6,6), 0.1f, 6, 6), false);
        new AutoStaticEntity(_entityManager, "wall3", new Box("wall3", new Vector3f(6,6,-0.1f), 6, 6, 0.1f), false);
        new AutoStaticEntity(_entityManager, "wall4", new Box("wall4", new Vector3f(6,6,12.1f), 6, 6, 0.1f), false);
        
		try {
			URL url = ResourceLocatorTool.locateResource(TYPE_MODEL, "room-tmp.jme");
			Node n = (Node) BinaryImporter.getInstance().load(url);

			List<Spatial> children = n.getChildren();

	        CullState cs = DisplaySystem.getDisplaySystem().getRenderer().createCullState();
	        cs.setCullMode(CullState.CS_BACK);

	        while (children.size() > 0) {
				Spatial s = children.remove(0);
				s.setRenderState(cs);
				s.updateRenderState();
//				AutoStaticEntity e = new AutoStaticEntity(_entityManager, s.getName(), s, false);
				_rootNode.attachChild(s);
	        }
		} catch (Exception e) {
			e.printStackTrace();
		}		
		
		initRoomObjects();
	}

	
    protected float getRandomSize(int min, int max) {
		return ((float) 1 + (Globals.random.nextInt(max-min)+min)) / 10.0f;
	}
	
    /*
	 * For each of the false axis values, a random value is chosen.
	 * To have a cube object, set x,y,z to all true
	 * @param x
	 * @param y
	 * @param z
	 * @param min
	 * 		the minimum size (1-25)
	 * @param max
	 * 		the maximum size (1-25)
	 */
	public Vector3f getSize(boolean x, boolean y, boolean z, int min, int max) {
		float size = getRandomSize(min, max);
		
		Vector3f vec = new Vector3f(size,size,size);
		if (x)
			vec.setX(getRandomSize(min, max));
		
		if (y)
			vec.setY(size);
		
		if (z)
			vec.setZ(size);
		
		return vec;
	}
	
	protected void initRoomObjects() {
		createGroup("group0", 0, new Vector3f(6,1,6), 1, 1, getSize(true,true,true,1,5));
		createGroup("group1", 0, new Vector3f(9,1,3), 1, 1, getSize(true,true,true,1,5));
		createGroup("group2", 0, new Vector3f(3,1,3), 1, 1, getSize(true,true,true,1,5));
		createGroup("group3", 0, new Vector3f(9,1,9), 1, 1, getSize(true,true,true,1,5));
		createGroup("group4", 0, new Vector3f(3,1,9), 1, 1, getSize(true,true,true,1,5));
	}
	
	protected void initInput() {
		super.initInput();
		_input = new InputHandler();

        PickerInputAction pia = new PickerInputAction(this, 'L', true);
        _input.addAction(pia, DEVICE_MOUSE, BUTTON_ALL, AXIS_NONE, false);

        final Tag jumpOverTag = new Tag("jump over");
        KeyBindingManager.getKeyBindingManager().set("tagJumpOver", KeyInput.KEY_O);
        KeyInputAction jumpOver = new KeyInputAction() {
        	public void performAction(InputActionEvent evt) {
        		if (jumpOverTag.isOpen()) {
        			jumpOverTag.closeDB();
        		} else {
        			jumpOverTag.openDB();
        		}
        	}
        };
        _input.addAction(jumpOver, "tagJumpOver", false);

        final Tag jumpOnTag = new Tag("jump on");
        KeyBindingManager.getKeyBindingManager().set("tagJumpOn", KeyInput.KEY_P);
        KeyInputAction jumpOn = new KeyInputAction() {
        	public void performAction(InputActionEvent evt) {
        		if (jumpOnTag.isOpen()) 
        			jumpOnTag.closeDB();
        		else
        			jumpOnTag.openDB();
        	}
        };
        _input.addAction(jumpOn, "tagJumpOn", false);
        
        
//        KeyBindingManager.getKeyBindingManager().set("jumpCorrector", KeyInput.KEY_NUMPAD2);
//        KeyInputAction jumpTo = new KeyInputAction() {
//        	public void performAction(InputActionEvent evt) {
//        		_wubble.jumpTo(new Vector3f(6,3,6), false);
//        	}
//        };
//        _input.addAction(jumpTo, "jumpCorrector", false);
		
//        KeyBindingManager.getKeyBindingManager().set("corrector", KeyInput.KEY_NUMPAD3);
//        KeyInputAction moveTo = new KeyInputAction() {
//        	public void performAction(InputActionEvent evt) {
//        		_wubble.moveTo(new Vector3f(6,0.12f,6), false);
//        	}
//        };
//        _input.addAction(moveTo, "corrector", false);
	}
	
	/**
	 * overrides the VisualGameState initCamera in order
	 * to set the frustum far clipping pane a long ways away.
	 */
	protected void initCamera() {
		super.initCamera();
		_camera.setLocation(new Vector3f(6f, 7.5f, -7));
		_camera.lookAt(new Vector3f(6.0f, 0.0f, 6.0f), new Vector3f(0,1,0));
		_camera.update();
		_camera.setFrustumFar(10000);
	}
	
	protected void initChaseCamera() {
		
		Vector3f targetOffset = new Vector3f(0,1.0f,0);
        HashMap<String,Object> props = new HashMap<String,Object>();
        props.put(ChaseCamera.PROP_TARGETOFFSET, targetOffset);
        props.put(ChaseCamera.PROP_STAYBEHINDTARGET, true);
        props.put(ChaseCamera.PROP_MINDISTANCE, "0.1");
        props.put(ChaseCamera.PROP_MAXDISTANCE, "11");

//        _chaseCamera = new ChaseCamera(_camera, _wubble.getVisualNode(), props);
        _chaseCamera = new ChaseCamera(_camera, _auto.getNode(), props);
        _chaseCamera.setActionSpeed(3.0f);
        
        props = new HashMap<String,Object>();
        props.put(ThirdPersonMouseLook.PROP_MOUSEBUTTON_FOR_LOOKING, 1);
        props.put(ThirdPersonMouseLook.PROP_INVERTEDY, true);
        props.put(ThirdPersonMouseLook.PROP_MINROLLOUT, 0.1f);
        props.put(ThirdPersonMouseLook.PROP_MAXROLLOUT, 11f);
        props.put(ThirdPersonMouseLook.PROP_MOUSEROLLMULT, 1.5f);
        props.put(ThirdPersonMouseLook.PROP_MOUSEXMULT, 2);
        props.put(ThirdPersonMouseLook.PROP_MOUSEYMULT, 2);
        
        _chaseCamera.getMouseLook().updateProperties(props);
        
        float speed = _chaseCamera.getMouseLook().getSpeed();
        _chaseCamera.getMouseLook().setSpeed(speed / 2.0f);
	}
	
	/**
	 * add 2 default lights to the scene.
	 *
	 */
	protected void initLights() {
	    _lightState = DisplaySystem.getDisplaySystem().getRenderer().createLightState();
	    _lightState.setEnabled( true );

	    PointLight light = new PointLight();
	    light.setDiffuse( new ColorRGBA( 1.0f, 1.0f, 1.0f, 1.0f ) );
	    light.setAmbient( new ColorRGBA( 0.5f, 0.5f, 0.5f, 0.2f ) );
	    light.setLocation( new Vector3f( 6.0f, 3, 3.0f ) );
	    light.setShadowCaster(true);
	    light.setEnabled( true );
	    _lightState.attach(light);

	    light = new PointLight();
	    light.setDiffuse( new ColorRGBA( 0.75f, 0.75f, 0.75f, 0.75f ) );
	    light.setAmbient( new ColorRGBA( 0.5f, 0.5f, 0.5f, 0.2f ) );
	    light.setLocation( new Vector3f( 6.0f, 2, 9.0f ) );
	    light.setShadowCaster(true);
	    light.setEnabled( true );
	    _lightState.attach(light);

        //_lightState.setGlobalAmbient(new ColorRGBA(0.6f, 0.6f, 0.6f, 1.0f));
	    
	    /** Attach the light to a lightState and the lightState to rootNode. */
	    _rootNode.setRenderState(_lightState);
		_rootNode.updateRenderState();
	}
	
	public void setInputEnabled(boolean enabled) {
		super.setInputEnabled(enabled);
		//_wubble.setInputEnabled(enabled);
		if (enabled) 
			_input.addAction(_switchAction, "gotoChat", false);
	}
	
	public void update(float tpf) {
//		System.out.println("Update: " + TimeManager.inst().getLogicalTime());
		StateDatabase.inst().beginTransaction();
		Swift.inst().preUpdate();
		_entityManager.preUpdate();
		
		super.update(tpf);
		
		// explicit update for Wubble and other objects that need to
		// fix stuff during the update cycle and can't be controlled
		// with a controller
		_entityManager.updateEntities(tpf);
		
		// prepare the camera for its updates
		_rootNode.updateWorldData(tpf);
		// why do we have to do this twice... some dependency that I don't understand
//		_wubble.getNode().updateWorldData(tpf);
		_auto.getNode().updateWorldData(tpf);
		_chaseCamera.update(tpf);

		Highlighter.update(tpf);
		Swift.inst().update();
		
		// delayed update is used for recording purposes.  We need
		// to delay these calls until all objects have finished
		// local updates (movement and the like)
		// Entity A may update a fluent for Entity B, so we can't
		// close any fluents until this is completed.
		for (DynamicEntity de : _entityManager.getAllDynamicEntities()) 
			de.delayedUpdate();
		
		// postUpdate will handle controllers that must wait until
		// all of the other wubbles have updated before running 
		// their test functions... a good example is approaching.
		// This will optimize the number of checks we do to keep
		// them minimal and allow us to do things like gjk
		for (DynamicEntity de : _entityManager.getAllDynamicEntities()) 
			de.postUpdate();

		_entityManager.getWorldDelta();
		
		StateDatabase.inst().endTransaction();
		
		// now that the state has become fairly consistent
		// we can allow the wubble to think about their next move
		// alternatively, the wubble can think first, but
		// it's 6 of one half a dozen of the other.
//		_wubble.think();
		_auto.think();
	}
	
	public boolean isClickable(String name) {
		Entity e = _entityManager.getEntity(name);
		if (e != null && e.isPickable())
			return true;
		return false;
	}
	
	/**
	 * The spatial that the user clicked on or null if they
	 * didn't click on anything.
	 * @param s
	 */
	public void picked(Spatial s) {
		if (s == null) {
			return;
		}
		if (_requestInput) {
			Entity e = _entityManager.getEntity(s.getName());
			if (e == null || !e.isPickable()) 
				return;
			
		}
	}
	
	public Node getPickNode() {
		return _rootNode;
	}
	
//	public void sendInitialMsg() {
//		StringBuffer msg = new StringBuffer("(");
//		for (Entity e : _entityManager.getAllEntities()) {
//			if (e.isPickable())
//				e.lispInitMessage(msg);
//		}
//		_wubble.lispInitMessage(msg);
//		msg.append(")");
//	}	
	
	public void logUpdates() {
		// for now logging will be taken care of here
		// I would like something more clever than this
		// but I'm tired right now.
		
		StringBuffer msg = new StringBuffer("<update time=\"" + TimeManager.inst().getElapsedTime() +
				"\" logicalTime=\"" + TimeManager.inst().getLogicalTime() + "\">");
		//boolean actuallySend = false;
		for (DynamicEntity de : _entityManager.getAllDynamicEntities()) {
			StringBuffer xml = de.xmlUpdateMessage();
			if (xml.length() > 0) {
				msg.append(xml);
		//		actuallySend = true;
			}
		}
		msg.append("</update>");
		
	}
	
	public void cleanup() {

		for (Entity e : _entityManager.getAllEntities()) {
			e.remove();
		}
		StateDatabase.inst().closeSession();
		StateDatabase.inst().endTransaction();
		Swift.inst().cleanup();
		
		Globals.USE_DATABASE = false;
		Globals.LOG_DYNAMIC = false;
	}
	
	public void giveColor(Spatial s, ColorRGBA color) {
        MaterialState ms = DisplaySystem.getDisplaySystem().getRenderer().createMaterialState();
        ms.setDiffuse(color);
        ms.setAmbient(ColorRGBA.black);
        ms.setEmissive(ColorRGBA.black);
        s.setRenderState(ms);
        s.updateRenderState();
	}	
	
	protected void createGroup(String name, int type, Vector3f origin, int rows, int columns, Vector3f size) {
		float x = origin.x;
		float y = (size.y / 2.0f) + 2;
		float z = origin.z;
		
		int count = 0;
		for (int i = 0; i < columns; ++i) {
			z = origin.z;
			for (int j = 0; j < rows; ++j) {
				PhysicsEntity de = null;
				switch (type) {
				case 0:  // box
					de = _entityManager.createDynamicBox(name+count, size);
					break;
				case 1:  // cylinder
					de = _entityManager.createDynamicCylinder(name+count, size.x, size.y);
					break;
				case 2:  // sphere
					de = _entityManager.createDynamicSphere(name+count, size.x);
					break;
				case 3:  // cone
					de = _entityManager.createDynamicCone(name+count, size.x, size.y);
				}
				
				de.setColor(ColorRGBA.randomColor());
				de.setPosition(new Vector3f(x,y,z));
				
				_rootNode.attachChild(de.getNode());

				z -= (size.z + 0.05f);
				++count;
			}
			x += (size.x + 0.2f);
		}
	}

	@Override
	public float getPctDone() {
		// TODO Auto-generated method stub
		return 0;
	}
	
}
