package edu.isi.wubble.wubbleroom;

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
import com.jme.renderer.Renderer;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jme.scene.shape.Box;
import com.jme.scene.shape.Sphere;
import com.jme.scene.state.AlphaState;
import com.jme.scene.state.CullState;
import com.jme.scene.state.LightState;
import com.jme.scene.state.MaterialState;
import com.jme.system.DisplaySystem;
import com.jme.util.export.binary.BinaryImporter;
import com.jme.util.resource.ResourceLocatorTool;
import com.jmex.physics.StaticPhysicsNode;

import edu.isi.wubble.Constants;
import edu.isi.wubble.gamestates.PhysicsGameState;
import edu.isi.wubble.physics.entity.DynamicEntity;
import edu.isi.wubble.physics.entity.Entity;
import edu.isi.wubble.physics.entity.EntityManager;
import edu.isi.wubble.physics.entity.StaticEntity;
import edu.isi.wubble.util.Globals;
import edu.isi.wubble.util.Highlighter;
import edu.isi.wubble.util.LispUtils;
import edu.isi.wubble.util.PickerI;
import edu.isi.wubble.util.PickerInputAction;
import edu.isi.wubble.util.PrepSelector;

public abstract class RoomState extends PhysicsGameState implements PickerI {
	protected ChaseCamera      _chaseCamera;

	protected LightState       _lightState;
	
	protected StaticPhysicsNode _roomNode;
	protected WubbleEntity       _wubble;
	
	protected Node              _searchNode;
	protected long              _lastUpdate;
	
	protected boolean           _requestInput;
	
	protected PrepSelector      _prepSelector;
	protected Spatial           _prepAreaSpatial;
	protected Spatial           _prepPointSpatial;
	
	protected boolean           _canSendUpdates;
	
	protected EntityManager     _entityManager;
	
	public RoomState() {
		super();
		init();
	}
	
	protected void init() {
		Globals.LOG_DYNAMIC = false;
		Globals.USE_DATABASE = false;
		Globals.SHANE_PRINTING = false;

		_drawPhysics = false;
		_renderStatistics = false;
		_physics.setAutoRestThreshold(0.05f);

		DisplaySystem.getDisplaySystem().getRenderer().getQueue().setTwoPassTransparency(false);

		_entityManager = new EntityManager(_physics, _input, _rootNode);
		
		initLights();
		initChaseCamera();
		initRoom();
		
        _searchNode = new Node("search");
        giveColor(_searchNode, ColorRGBA.red);
        _searchNode.updateRenderState();
        _rootNode.attachChild(_searchNode);
        
        _rootNode.updateWorldVectors();
		_rootNode.updateGeometricState(0, true);
		_rootNode.updateRenderState();
	
		_timer.reset();
	}
	
	protected void initRoom() {
        _wubble = new WubbleEntity(_entityManager, "jean");
		_wubble.setPosition(new Vector3f(7.5f, 3.0f, 7.5f));
        _rootNode.attachChild(_wubble.getNode());
        
		try {
			URL url = ResourceLocatorTool.locateResource(TYPE_MODEL, "room-tmp.jme");
			Node n = (Node) BinaryImporter.getInstance().load(url);

			List<Spatial> children = n.getChildren();

	        CullState cs = DisplaySystem.getDisplaySystem().getRenderer().createCullState();
	        cs.setCullMode(CullState.CS_BACK);

	        while (children.size() > 0) {
				Spatial s = children.remove(0);
				StaticEntity e = new StaticEntity(_entityManager, s.getName(), s, false);
				_rootNode.attachChild(e.getNode());
	        }
		} catch (Exception e) {
			e.printStackTrace();
		}		
        
		_prepSelector = new PrepSelector(this);
		_prepSelector.setBoundaries(0, 12, 0, 12);
		_prepSelector.setEnabled(false);
		_rootNode.attachChild(_prepSelector);
		
		_prepAreaSpatial = new Box("prepArea", new Vector3f(0,0,0), 0.5f, 0.5f, 0.5f);
		MaterialState ms = DisplaySystem.getDisplaySystem().getRenderer().createMaterialState();
		ms.setDiffuse(new ColorRGBA(1.0f,1.0f,1.0f,0.9f));
		ms.setAmbient(Constants.COLOR_OFF);
		ms.setEmissive(Constants.COLOR_OFF);
		
		AlphaState as = DisplaySystem.getDisplaySystem().getRenderer().createAlphaState();
		as.setBlendEnabled(true);
		as.setTestEnabled(true);
		as.setTestFunction(AlphaState.TF_GREATER);
		
		CullState cs = DisplaySystem.getDisplaySystem().getRenderer().createCullState();
		cs.setCullMode(CullState.CS_BACK);
		
		_prepAreaSpatial.setRenderState(ms);
		_prepAreaSpatial.setRenderState(as);
		_prepAreaSpatial.setRenderState(cs);
		_prepAreaSpatial.setCullMode(Spatial.CULL_ALWAYS);
		_prepAreaSpatial.setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
		_rootNode.attachChild(_prepAreaSpatial);
		
		_prepPointSpatial = new Sphere("prepPoint", new Vector3f(0,0,0), 10, 10, 0.05f);
		_prepPointSpatial.setRenderState(ms);
		_prepPointSpatial.setCullMode(Spatial.CULL_ALWAYS);
		_rootNode.attachChild(_prepPointSpatial);
		
		initRoomObjects();
	}
	
	protected abstract void initRoomObjects();
	
	protected void initInput() {
		super.initInput();
		_input = new InputHandler();
		addScreenShot(KeyInput.KEY_PGDN);

        PickerInputAction pia = new PickerInputAction(this, 'L', true);
        _input.addAction(pia, DEVICE_MOUSE, BUTTON_ALL, AXIS_NONE, false);
		
        KeyBindingManager.getKeyBindingManager().set("jumpCorrector", KeyInput.KEY_NUMPAD2);
        KeyInputAction jumpTo = new KeyInputAction() {
        	public void performAction(InputActionEvent evt) {
        		_wubble.jumpTo(new Vector3f(6,3,6), false);
        	}
        };
        _input.addAction(jumpTo, "jumpCorrector", false);
		
        KeyBindingManager.getKeyBindingManager().set("corrector", KeyInput.KEY_NUMPAD3);
        KeyInputAction moveTo = new KeyInputAction() {
        	public void performAction(InputActionEvent evt) {
        		_wubble.moveTo(new Vector3f(6,0.12f,6), false);
        	}
        };
        _input.addAction(moveTo, "corrector", false);
        
		KeyBindingManager.getKeyBindingManager().set("moveMouse", KeyInput.KEY_NUMPAD4);
    	KeyInputAction mouseListener = new KeyInputAction() {
    		public void performAction(InputActionEvent evt) {
    			_prepSelector.setEnabled(!_prepSelector.getEnabled());
    		}
    	};		
    	_input.addAction(mouseListener, "moveMouse", false);

    	addChatSwitchAbility(RoomManager.getChat().getName());
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
		Sphere s = new Sphere("chase", new Vector3f(0,0,0), 5, 5, 0.5f);
		s.setLocalTranslation(6,0,6);  // middle of the room
		s.setCullMode(Spatial.CULL_ALWAYS);
		_rootNode.attachChild(s);
		_rootNode.updateRenderState();
		
		Vector3f targetOffset = new Vector3f(0,1.0f,0);
        HashMap<String,Object> props = new HashMap<String,Object>();
        props.put(ChaseCamera.PROP_TARGETOFFSET, targetOffset);
        props.put(ChaseCamera.PROP_STAYBEHINDTARGET, true);
        props.put(ChaseCamera.PROP_MINDISTANCE, "0.1");
        props.put(ChaseCamera.PROP_MAXDISTANCE, "11");

        _chaseCamera = new ChaseCamera(_camera, s, props);
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
		_wubble.setInputEnabled(enabled);
		if (enabled) 
			_input.addAction(_switchAction, "gotoChat", false);
	}
	
	public void beginSendingUpdates() {
		_canSendUpdates = true;
	}
	
	public void update(float tpf) {
		super.update(tpf);
		_chaseCamera.update(tpf);
		_wubble.update(tpf);
		
		_rootNode.updateGeometricState(tpf, true);
		
		Highlighter.update(tpf);
		if (_canSendUpdates && System.currentTimeMillis() - _lastUpdate > 200) {
			sendUpdateMsg();
			_lastUpdate = System.currentTimeMillis();
		}
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
			
			RoomManager.getChat().systemMessage("You point to it for your wubble.");
			SocketClient.inst().sendMessage("response " + e.getName());
		}
	}
	
	public Node getPickNode() {
		return _rootNode;
	}
	
	
	// send messages for all rooms. Lisp needs this information
	// regardless of what room it is in.

	public void sendInitialSceneMsg() {
		String msg = "((min-pos (0 0 0)) (max-pos (12 12 12))" +
				"(eye-pos " + LispUtils.toLisp(_camera.getLocation()) + ")" +
				"(look-at " + LispUtils.toLisp(_camera.getDirection()) + "))";
		SocketClient.inst().sendMessage("scene-info " + msg);
	}
	
	public void sendInitialMsg() {
		StringBuffer msg = new StringBuffer("(");
		for (Entity e : _entityManager.getAllEntities()) {
			if (e.isPickable())
				e.lispInitMessage(msg);
		}
		_wubble.lispInitMessage(msg);
		msg.append(")");
		SocketClient.inst().sendMessage("scene-init " + msg.toString());
	}	
	
	public void sendUpdateMsg() {
		StringBuffer msg = new StringBuffer("(");
		boolean actuallySend = false;
		for (DynamicEntity de : _entityManager.getAllDynamicEntities()) {
			if (de.lispUpdateMessage(msg)) 
				actuallySend = true;
		}
		msg.append(")");
		if (actuallySend) 
			SocketClient.inst().sendMessage("scene-update " + msg.toString());
	}
	
	public void cleanup() {
		_canSendUpdates = false;
		SocketClient.inst().close();
		Highlighter.deactivateAll();
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
				ObjectEntity oe = null;
				switch (type) {
				case 0:  // box
					oe = ObjectEntity.createDynamicBox(_entityManager, name + count, size);
					break;
				case 1:  // cylinder
					oe = ObjectEntity.createCylinder(_entityManager, name + count, size.x, size.y);
					break;
				case 2:  // sphere
					oe = ObjectEntity.createSphere(_entityManager, name + count, size.x);
					break;
				case 3:  // cone
					oe = ObjectEntity.createCone(_entityManager, name + count, size.x, size.y);
				}
				
				oe.applyRandomColor();
				oe.setPosition(new Vector3f(x,y,z));
				_rootNode.attachChild(oe.getNode());

				z -= (size.z + 0.2f);
				++count;
			}
			x += (size.x + 0.2f);
		}
	}
	
}
