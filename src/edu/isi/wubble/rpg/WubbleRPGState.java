package edu.isi.wubble.rpg;

import static com.jme.input.InputHandler.AXIS_NONE;
import static com.jme.input.InputHandler.BUTTON_ALL;
import static com.jme.input.InputHandler.DEVICE_MOUSE;
import static com.jme.util.resource.ResourceLocatorTool.TYPE_MODEL;
import static edu.isi.wubble.jgn.rpg.RPGPhysics.PICKER;
import static edu.isi.wubble.jgn.rpg.RPGPhysics.SHOOTER;
import static edu.isi.wubble.jgn.message.InvokeMessage.createMsg;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import com.captiveimagination.jgn.event.MessageListener;
import com.captiveimagination.jgn.message.Message;
import com.captiveimagination.jgn.synchronization.message.Synchronize3DMessage;
import com.jme.bounding.BoundingBox;
import com.jme.input.ChaseCamera;
import com.jme.input.InputHandler;
import com.jme.input.KeyBindingManager;
import com.jme.input.KeyInput;
import com.jme.input.action.InputAction;
import com.jme.input.action.InputActionEvent;
import com.jme.input.action.KeyInputAction;
import com.jme.light.PointLight;
import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jme.scene.TriMesh;
import com.jme.scene.state.AlphaState;
import com.jme.scene.state.CullState;
import com.jme.scene.state.FogState;
import com.jme.scene.state.LightState;
import com.jme.system.DisplaySystem;
import com.jme.util.GameTaskQueueManager;
import com.jme.util.export.binary.BinaryImporter;
import com.jme.util.resource.ResourceLocatorTool;
import com.jmex.effects.particles.ParticleFactory;
import com.jmex.effects.particles.ParticleInfluence;
import com.jmex.effects.particles.ParticleMesh;
import com.jmex.effects.particles.SimpleParticleInfluenceFactory;

import edu.isi.wubble.Main;
import edu.isi.wubble.character.WubbleCharacter;
import edu.isi.wubble.character.WubbleCrossbowCharacter;
import edu.isi.wubble.gamestates.VisualGameState;
import edu.isi.wubble.jgn.ClientInput;
import edu.isi.wubble.jgn.ConnectionManager;
import edu.isi.wubble.jgn.message.HiddenUpdateMessage;
import edu.isi.wubble.jgn.message.InvokeMessage;
import edu.isi.wubble.jgn.message.WorldUpdateMessage;
import edu.isi.wubble.util.Highlighter;
import edu.isi.wubble.util.PickerI;
import edu.isi.wubble.util.PickerInputAction;

public class WubbleRPGState extends VisualGameState implements PickerI,MessageListener {

	protected ClientInput _clientInput;
	protected ChaseCamera _chaseCamera;
	
	protected LightState     _lightState;
	protected FogState       _fogState;
	
	protected HashMap<String,Spatial>      _pickableMap;
	protected HashMap<String,Spatial>      _entityMap;
	protected HashMap<String,ParticleMesh> _explodeMap;
	
	protected TreeMap<Integer,WubbleCharacter> _wubbleMap;
	
	protected HashMap<Short,String>               _serverMap;
	protected HashMap<Short,Synchronize3DMessage> _messageMap;
	
	protected WorldUpdateMessage _lastMessage;
	protected Object _lock = new Object();

	protected HiddenUpdateMessage _lastHiddenMessage;
	protected Object _hiddenLock = new Object();
	
	protected boolean _canUpdate = false;
	
	public WubbleRPGState() {
		super();
		
		_pickableMap = new HashMap<String,Spatial>();
		_entityMap = new HashMap<String,Spatial>();
		_explodeMap = new HashMap<String,ParticleMesh>();
	
		_wubbleMap = new TreeMap<Integer,WubbleCharacter>();
		
		_serverMap = new HashMap<Short,String>();
		_messageMap = new HashMap<Short,Synchronize3DMessage>();
		
		init();
	}
	
	protected void init() {
		_renderStatistics = false;
		//_showBounding = true;
		initLights();
		
		initStatic();
		initDynamic();
		
		initWubbles();
		initEnemies();
		
		initCoins();
		
		initChaseCamera();
				
        CullState cs = DisplaySystem.getDisplaySystem().getRenderer().createCullState();
        cs.setCullMode(CullState.CS_BACK);
        _rootNode.setRenderState(cs);
        _rootNode.updateRenderState();

        AlphaState as1 = DisplaySystem.getDisplaySystem().getRenderer().createAlphaState();
        as1.setBlendEnabled(true);
        as1.setSrcFunction(AlphaState.SB_SRC_ALPHA);
        as1.setDstFunction(AlphaState.DB_ONE_MINUS_SRC_ALPHA);
        as1.setEnabled(true);
        _rootNode.setRenderState(as1);
        
        _fogState = DisplaySystem.getDisplaySystem().getRenderer().createFogState();
        _fogState.setDensity(0.75f);
        _fogState.setEnabled(true);
        _fogState.setColor(new ColorRGBA(0.0f, 0.0f, 0.0f, 0.0f));
        _fogState.setEnd(15);
        _fogState.setStart(5);
        _fogState.setDensityFunction(FogState.DF_LINEAR);
        _fogState.setApplyFunction(FogState.AF_PER_PIXEL);
        _rootNode.setRenderState(_fogState);
		
		_rootNode.updateGeometricState(0, true);
		_rootNode.updateRenderState();

	}
	
	/**
	 * initialize the static components of the scene
	 * and generate their physics.
	 *
	 */
	protected void initStatic() {
		try {
			URL url = ResourceLocatorTool.locateResource(TYPE_MODEL, "rpg-cavern.jme");
			Node levelNode = (Node) BinaryImporter.getInstance().load(url);
			levelNode.lockBounds();
			levelNode.lockTransforms();
			levelNode.lockBranch();
			
			_entityMap.put(levelNode.getName(), levelNode);
			_rootNode.attachChild(levelNode);
		} catch (Exception e) {
			e.printStackTrace();
		}		
		
		try {
			URL url = ResourceLocatorTool.locateResource(TYPE_MODEL, "rpg-static.jme");
			Node staticNode = (Node) BinaryImporter.getInstance().load(url);
			List<Spatial> children = staticNode.getChildren();
			while (children.size() > 0) {
				Spatial s = children.remove(0);
				s.setModelBound(new BoundingBox());
				s.updateModelBound();
				s.lockBounds();
				s.lockTransforms();
				s.lockBranch();
				
				//_shadowPass.addOccluder(s);
				_entityMap.put(s.getName(), s);
				_pickableMap.put(s.getName(), s);
				
				_rootNode.attachChild(s);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}

	/**
	 * initialize the dynamic elements of the scenes
	 * those without controllers.
	 *
	 */
	protected void initDynamic() {
		try {
			URL url = ResourceLocatorTool.locateResource(TYPE_MODEL, "rpg-dynamic.jme");
			Node staticNode = (Node) BinaryImporter.getInstance().load(url);
			Iterator<Spatial> iter = staticNode.getChildren().iterator();
			while (iter.hasNext()) {
				Node n = (Node) iter.next();
				List<Spatial> children = n.getChildren();
				while (children.size() > 0) {
					Spatial s = children.remove(0);
					s.setLocalRotation(new Quaternion());
					s.setLocalTranslation(new Vector3f());
					s.setModelBound(new BoundingBox());
					s.updateModelBound();
					
					_entityMap.put(s.getName(), s);
					_pickableMap.put(s.getName(), s);

					_rootNode.attachChild(s);
				}
			}

			url = ResourceLocatorTool.locateResource(TYPE_MODEL, "arrow.jme");
			Node n = (Node) BinaryImporter.getInstance().load(url);
			_entityMap.put(n.getName(), n);
			_rootNode.attachChild(n);
			
		} catch (Exception e) {
			e.printStackTrace();
		}		    
		
	}
		
	/**
	 * create all the stitches and put them in their correct 
	 * places.
	 *
	 */
	protected void initEnemies() {
		try {
			URL url = ResourceLocatorTool.locateResource(TYPE_MODEL, "rpg-stitches.jme");
			Node stitchesNode = (Node) BinaryImporter.getInstance().load(url);
			List<Spatial> children = stitchesNode.getChildren();
			while (children.size() > 0) {
				Spatial s = children.remove(0);
				s.setModelBound(new BoundingBox());
				s.updateModelBound();
				
				System.out.println("adding: " + s.getName());
				_entityMap.put(s.getName(), s);
				_rootNode.attachChild(s);
				
				buildParticleMesh((TriMesh) s, 0.5f);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}			
	}
	
	/**
	 * should really read from a file, but right now I create
	 * two wubbles in two different spaces and give them
	 * generic names.
	 */
	protected void initWubbles() {
		WubbleCharacter wc = new WubbleCrossbowCharacter("shooter");
		_wubbleMap.put(SHOOTER, wc);
		_entityMap.put(wc.getName(), wc.getVisualNode());
		_rootNode.attachChild(wc);

		wc = new WubbleCharacter("picker");
		_wubbleMap.put(PICKER, wc);
		// we have to store the visual node because we rotate it already
		_entityMap.put(wc.getName(), wc.getVisualNode());
		_rootNode.attachChild(wc);

		_rootNode.updateRenderState();
	}
	
	/**
	 * initialize the dynamic elements of the scenes
	 * those without controllers.
	 *
	 */
	protected void initCoins() {
		try {
			URL url = ResourceLocatorTool.locateResource(TYPE_MODEL, "rpg-coins.jme");
			Node coinsNode = (Node) BinaryImporter.getInstance().load(url);
			List<Spatial> children = coinsNode.getChildren();
			for (int i = 1; i < 6; ++i) {
				Spatial s = children.remove(0);
				s.setName("coin" + i);
				s.setLocalRotation(new Quaternion());
				s.setLocalTranslation(new Vector3f());
					
				_entityMap.put(s.getName(), s);
				_pickableMap.put(s.getName(), s);
				
				buildParticleMesh((TriMesh) s, 1.0f);

				_rootNode.attachChild(s);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}		    
		
	}		
	
	protected void buildParticleMesh(TriMesh mesh, float size) {
        ParticleMesh pMesh = ParticleFactory.buildBatchParticles("particles", mesh.getBatch(0));
        pMesh.setEmissionDirection(new Vector3f(1, 1, 1));
        pMesh.setOriginOffset(new Vector3f(0, 0, 0));
        pMesh.setInitialVelocity(.002f);
        pMesh.setStartSize(size);
        pMesh.setEndSize(size);
        pMesh.setMinimumLifeTime(1000f);
        pMesh.setMaximumLifeTime(5000f);
        pMesh.setStartColor(new ColorRGBA(1, 1, 1, 1));
        pMesh.setEndColor(new ColorRGBA(1, 1, 1, 0));
        pMesh.setMaximumAngle(0f * FastMath.DEG_TO_RAD);
        pMesh.setParticleSpinSpeed(180 * FastMath.DEG_TO_RAD);

        Vector3f up = new Vector3f(0,1,0);
        ParticleInfluence wind = SimpleParticleInfluenceFactory.createBasicWind(.008f, up, true, true);
        wind.setEnabled(true);
        pMesh.addInfluence(wind);

        pMesh.forceRespawn();
        pMesh.setModelBound(new BoundingBox());
        pMesh.updateModelBound();
        
        _explodeMap.put(mesh.getName(), pMesh);
	}
	
	protected void initInput() {
		super.initInput();
		_input = new InputHandler();
		addScreenShot(KeyInput.KEY_PGDN);
		
		_clientInput = new ClientInput(_input);
		
        PickerInputAction pia = new PickerInputAction(this);
        _input.addAction(pia, DEVICE_MOUSE, BUTTON_ALL, AXIS_NONE, false);
		
        KeyBindingManager.getKeyBindingManager().set("winner", KeyInput.KEY_NUMPAD1);
    	KeyInputAction kia = new KeyInputAction() {
    		public void performAction(InputActionEvent evt) {
    			System.out.println("actionFired:gotoChat");
    			Callable<?> callable = new Callable<Object>() {
					public Object call() throws Exception {
						playWinningState();
						return null;
					}
    			};
    			GameTaskQueueManager.getManager().update(callable);
    		}
    	};		
    	_input.addAction(kia, "winner", false);

        KeyBindingManager.getKeyBindingManager().set("loser", KeyInput.KEY_NUMPAD2);
    	KeyInputAction blah = new KeyInputAction() {
    		public void performAction(InputActionEvent evt) {
    			Callable<?> callable = new Callable<Object>() {
					public Object call() throws Exception {
						playLosingState();
						return null;
					}
    			};
    			GameTaskQueueManager.getManager().update(callable);
    		}
    	};		
    	_input.addAction(blah, "loser", false);
    	
    	addChatSwitchAbility(RPGManager.getChat().getName());
	}
	
	protected void addFireAbility() {
    	_input.addAction(new InputAction() {
            public void performAction( InputActionEvent evt ) {
            	if (evt.getTriggerCharacter() == 'L' && !evt.getTriggerPressed()) {
            		InvokeMessage m = createMsg("fireArrow", null);
            		m.sendToServer();
            	}
            }
        }, DEVICE_MOUSE, BUTTON_ALL, AXIS_NONE, false );
	}
	
	/**
	 * overrides the VisualGameState initCamera in order
	 * to set the frustum far clipping pane a long ways away.
	 */
	protected void initCamera() {
		super.initCamera();
		_camera.setFrustumFar(15);
	}
	
	@SuppressWarnings("unchecked")
	protected void initChaseCamera() {
		Vector3f targetOffset = new Vector3f(0,1.0f,0);
        HashMap props = new HashMap();
        props.put(ChaseCamera.PROP_TARGETOFFSET, targetOffset);
        props.put(ChaseCamera.PROP_STAYBEHINDTARGET, "true");
        props.put(ChaseCamera.PROP_MINDISTANCE, "0.1");
        props.put(ChaseCamera.PROP_MAXDISTANCE, "4");

        _chaseCamera = new ChaseCamera(_camera, _wubbleMap.get(SHOOTER).getVisualNode(), props);
        _chaseCamera.setLooking(false);
        _chaseCamera.getMouseLook().setEnabled(false);
        _chaseCamera.setEnabledOfAttachedHandlers(false);
        _chaseCamera.setEnableSpring(false);     
	}
	
	/**
	 * add 2 default lights to the scene.
	 *
	 */
	protected void initLights() {
	    _lightState = DisplaySystem.getDisplaySystem().getRenderer().createLightState();
	    _lightState.setEnabled( true );

	    /** Set up a basic, default light. */
	    PointLight light = new PointLight();
	    light.setDiffuse( new ColorRGBA( 0.75f, 0.75f, 0.75f, 0.75f ) );
	    light.setAmbient( new ColorRGBA( 0.5f, 0.5f, 0.5f, 1.0f ) );
	    light.setLocation( new Vector3f( -3, 3, 11 ) );
	    light.setConstant(0.9f);
	    light.setLinear(0.5f);
	    light.setQuadratic(1);
	    light.setEnabled( true );
	    _lightState.attach(light);
	    
	    light = new PointLight();
	    light.setDiffuse( new ColorRGBA( 0.75f, 0.75f, 0.75f, 0.75f ) );
	    light.setAmbient( new ColorRGBA( 0.5f, 0.5f, 0.5f, 1.0f ) );
	    light.setLocation( new Vector3f( -3, 20, -80) );
	    light.setConstant(0.9f);
	    light.setLinear(0.5f);
	    light.setQuadratic(3);
	    light.setEnabled( true );
	    _lightState.attach( light );

        _lightState.setGlobalAmbient(new ColorRGBA(0.6f, 0.6f, 0.6f, 1.0f));
        
	    /** Attach the light to a lightState and the lightState to rootNode. */
	    _rootNode.setRenderState(_lightState);
		_rootNode.updateRenderState();
	}
	
	public void setCanUpdate(boolean status) {
		InvokeMessage msg = createMsg("ready", new Object[] { ConnectionManager.inst().getClientId() });
		msg.sendToServer();

		if (RPGManager.getRole() != SHOOTER) {
			System.out.println("switching");
			_clientInput.setWubbleRole("picker");
			_chaseCamera.setTarget(_wubbleMap.get(PICKER).getVisualNode());
		} else {
			addFireAbility();
			_clientInput.setWubbleRole("shooter");
		}

		_canUpdate = status;
		if (_canUpdate)
			_timer.reset();
	}
	
	private long _updateElapsed;
	private int _updateCount;
	public void update(float tpf) {
		long start = System.currentTimeMillis();
		super.update(tpf);

		_clientInput.update(tpf);

		processUpdate();
		//processHiddenUpdate();

		for (WubbleCharacter wc : _wubbleMap.values()) {
			wc.update(tpf);
		}
				
		Highlighter.update(tpf);

		_chaseCamera.getTarget().updateGeometricState(tpf,true);
		_chaseCamera.update(tpf);

		_rootNode.updateGeometricState(tpf, true);

		_updateElapsed += System.currentTimeMillis() - start;
		++_updateCount;
		if (_updateCount > 0 && _updateCount %100 == 0) {
			//System.out.println("Average Update: " + ((float) _updateElapsed / (float) _updateCount));
			_updateElapsed = 0;
			_updateCount = 0;
		}
	}
	
	private long _renderElapsed;
	private int _renderCount;
	public void render(float tpf) {
		long start = System.currentTimeMillis();
		super.render(tpf);
		_renderElapsed += System.currentTimeMillis() - start;
		++_renderCount;
		if (_renderCount > 0 && _renderCount %100 == 0) {
			//System.out.println("Average Render: " + ((float) _renderElapsed / (float) _renderCount));
		}
	}
	
	private static HashSet<String> _unknownSet = new HashSet<String>();
	protected void processUpdate() {
		HashMap<Short,Synchronize3DMessage> msgMap = null;
		synchronized (_lock) {
			msgMap = _messageMap;
			_messageMap = new HashMap<Short,Synchronize3DMessage>();
		}
		
		for (Synchronize3DMessage msg : msgMap.values()) {
			String name = _serverMap.get(msg.getSyncObjectId());
			Spatial s = _entityMap.get(name);
			if (s != null) {
				s.setLocalTranslation(msg.getPositionX(), msg.getPositionY(), msg.getPositionZ());
				s.setLocalRotation(new Quaternion(msg.getRotationX(), msg.getRotationY(), 
						msg.getRotationZ(), msg.getRotationW()));
				s.updateWorldVectors();
			} else {
				if (!_unknownSet.contains(name)) {
					System.out.println("unknown spatial: " + name);
					_unknownSet.add(name);
				}
			}
		}
	}

	/**
	 * process a message that has the hidden items 
	 * locations inside of it.
	 */
	protected void processHiddenUpdate() {
		if (_lastHiddenMessage == null) 
				return;

		ArrayList<String> names = null;
		ArrayList<Float> x,y,z,rotX,rotY,rotZ,rotW = null;
		synchronized (_hiddenLock) {
			names = _lastHiddenMessage.getNames();
			x = _lastHiddenMessage.getX();
			y = _lastHiddenMessage.getY();
			z = _lastHiddenMessage.getZ();
			rotX = _lastHiddenMessage.getRotX();
			rotY = _lastHiddenMessage.getRotY();
			rotZ = _lastHiddenMessage.getRotZ();
			rotW = _lastHiddenMessage.getRotW();
		}
			
		for (int i = 0; i < names.size(); ++i) {
			Spatial s = _entityMap.get(names.get(i));
			if (s != null) {
				s.setLocalTranslation(new Vector3f(x.get(i),y.get(i),z.get(i)));
				s.setLocalRotation(new Quaternion(rotX.get(i), rotY.get(i), rotZ.get(i), rotW.get(i)));
				s.updateWorldVectors();
			} else {
				if (!_unknownSet.contains(names.get(i))) {
					System.out.println("unknown spatial: " + names.get(i));
					_unknownSet.add(names.get(i));
				}	
			}
		}
	}
	
	@Override
	public float getPctDone() {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * clean up the client connection
	 */
	public void cleanup() {
		ConnectionManager.inst().disconnect();
	}

	public Node getPickNode() {
		return _rootNode;
	}

	public boolean isClickable(String name) {
		if (_pickableMap.containsKey(name)) {
			return true;
		}
		return false;
	}

	public void picked(Spatial s) {
		short id = ConnectionManager.inst().getClientId();
		if (s == null) {
			InvokeMessage msg = createMsg("clearActiveChoice", new Object[] { id });
			msg.sendToServer();
		} else {
			InvokeMessage msg = createMsg("setActiveChoice", new Object[] { id, s.getName() });
			msg.sendToServer();
		}
	}		
	
	//--------------------------------------
	// callbacks for server
	//--------------------------------------
	
	public void mapServerId(String name, Short id) {
//		System.out.println("mapServerId: " + name + " " + id);
		_serverMap.put(id, name);
	}
	
	public void initialPosition(final String name, final Vector3f pos) {
//		System.out.println("initialPosition: " + name);
		Spatial s = _entityMap.get(name);
		if (s == null) {
			System.out.println("[initialPosition] unknown spatial: " + name);
			return;
		}
		s.setLocalTranslation(new Vector3f(pos));
	}
	
	public void initialRotation(final String name, final Quaternion q) {
//		System.out.println("initialRotation: " + name);
		Spatial s = _entityMap.get(name);
		if (s == null) {
			System.out.println("[initialRotation] unknown spatial: " + name);
			return;
		}
		s.setLocalRotation(new Quaternion(q));
		return;
	}
	
	public void addHighlight(String name, String spatialName) {
		if (!_pickableMap.containsKey(spatialName)) {
			System.out.println("[addHighlight] unknown pickable: " + spatialName);
			return;
		}
		Highlighter.createHighlighter(name, _pickableMap.get(spatialName));
	}
	
	public void clearHighlight(String name) {
		Highlighter.deactivateHighlighter(name);
	}
	
	/**
	 * this will play the desired animation on the wubble.
	 * @param wubbleId
	 * @param animationName
	 */
	public void play(Integer wubbleId, String animationName) {
		WubbleCharacter wc = _wubbleMap.get(wubbleId);
		wc.playAnimation(animationName);
	}
	
	
	/**
	 * remove the given entity from the render process.
	 * @param name
	 */
	public void removeEntity(final String name) {
		explodeSpatial(name);
	}
	
	protected void explodeSpatial(final String name) {
		ParticleMesh pMesh = _explodeMap.get(name);
		if (pMesh == null) {
			Spatial s = _entityMap.remove(name);
			s.removeFromParent();
			return;
		}
			
        _rootNode.attachChild(pMesh);		
        _rootNode.updateRenderState();
        
        Timer t = new Timer();
        t.schedule(new TimerTask() {
        	public void run() {
        		Callable<?> callable = new Callable<Object>() {
        			public Object call() throws Exception {
        				Spatial s = _entityMap.remove(name);
        				s.removeFromParent();
        				
        				ParticleMesh pMesh = _explodeMap.remove(name);
        				pMesh.removeFromParent();
        				return null;
        			}
        		};
        		GameTaskQueueManager.getManager().update(callable);
        	}
        }, 3000);
	}
	
	public void playWinningState() {
		RPGManager.stopRpgGame();

		RPGWinningState rws = (RPGWinningState) Main.inst().findOrCreateState(RPGWinningState.class.getName());
		rws.addEntities(_entityMap);
		rws.setWubbles(_wubbleMap.get(SHOOTER), _wubbleMap.get(PICKER));
		rws.activateState();
	}
	
	public void playLosingState() {
		RPGManager.stopRpgGame();
		
		RPGLosingState rls = (RPGLosingState) Main.inst().findOrCreateState(RPGLosingState.class.getName());
		rls.setActive(true);
	}
	
	
	//--------------------------------------
	// jgn callbacks
	//--------------------------------------
	
	public void messageCertified(Message message) { }
	public void messageFailed(Message message) { }
	public void messageSent(Message message) { }

	public void messageReceived(final Message message) {
		if (message instanceof InvokeMessage) {
			Callable<?> callable = new Callable<Object>() {
				public Object call() throws Exception {
					RPGManager.dispatchMessage((InvokeMessage) message);
					return null;
				}
			};
			GameTaskQueueManager.getManager().update(callable);
		} else if (message instanceof WorldUpdateMessage) {
			_lastMessage = ((WorldUpdateMessage) message);
		} else if (message instanceof HiddenUpdateMessage) {
			_lastHiddenMessage = ((HiddenUpdateMessage) message);
		} else if (message instanceof Synchronize3DMessage) {
			synchronized (_lock) {
				Synchronize3DMessage msg = (Synchronize3DMessage) message;
				_messageMap.put(msg.getSyncObjectId(), msg);
			}
		} 
	}
}
