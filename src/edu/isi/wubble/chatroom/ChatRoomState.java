package edu.isi.wubble.chatroom;

import static com.jme.util.resource.ResourceLocatorTool.TYPE_MODEL;

import java.net.URL;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import com.captiveimagination.jgn.event.MessageListener;
import com.captiveimagination.jgn.message.Message;
import com.captiveimagination.jgn.synchronization.message.Synchronize3DMessage;
import com.jme.input.ChaseCamera;
import com.jme.input.InputHandler;
import com.jme.input.KeyBindingManager;
import com.jme.input.KeyInput;
import com.jme.input.action.InputActionEvent;
import com.jme.input.action.KeyInputAction;
import com.jme.input.thirdperson.ThirdPersonMouseLook;
import com.jme.light.DirectionalLight;
import com.jme.light.PointLight;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Node;
import com.jme.scene.state.CullState;
import com.jme.scene.state.LightState;
import com.jme.system.DisplaySystem;
import com.jme.util.GameTaskQueueManager;
import com.jme.util.export.binary.BinaryImporter;
import com.jme.util.resource.ResourceLocatorTool;

import edu.isi.wubble.Main;
import edu.isi.wubble.WubbleManager;
import edu.isi.wubble.character.WubbleCharacter;
import edu.isi.wubble.gamestates.DefaultGuiGameState;
import edu.isi.wubble.gamestates.VisualGameState;
import edu.isi.wubble.jgn.ClientInput;
import edu.isi.wubble.jgn.ConnectionManager;
import edu.isi.wubble.jgn.message.InvokeMessage;
import edu.isi.wubble.util.Highlighter;

public class ChatRoomState extends VisualGameState implements MessageListener  {

	protected KeyInputAction _switchAction;
	protected ClientInput    _clientInput;
	
	protected ChaseCamera _chaseCamera;
	
	protected HashMap<Short,String>               _serverMap;
	protected HashMap<Short,Synchronize3DMessage> _messageMap;
	
	protected WubbleManager _wubbleManager;

	protected LightState _lightState;
	protected Node       _worldNode;
	
	protected Object     _lock;

	protected boolean    _canUpdate;
	
	public ChatRoomState() {
		super();
		_canUpdate = false;
		_serverMap  = new HashMap<Short,String>();
		_messageMap = new HashMap<Short,Synchronize3DMessage>();
		_lock = new Object();
	}
	
	public void setup(WubbleManager mgr) {
		_wubbleManager = mgr;
		_wubbleManager.setRootNode(_rootNode);
		init();
		_canUpdate = true;
	}

	protected void init() {
		try {
			URL url = ResourceLocatorTool.locateResource(TYPE_MODEL, "chatroom-world.jme");
			_worldNode = (Node) BinaryImporter.getInstance().load(url);
			_rootNode.attachChild(_worldNode);
		} catch (Exception e) {
			e.printStackTrace();
		}		
		
		_wubbleManager.setupPlayerWubble(ColorRGBA.yellow);
		
        CullState cs = DisplaySystem.getDisplaySystem().getRenderer().createCullState();
        cs.setCullMode(CullState.CS_BACK);
        _rootNode.setRenderState(cs);

		initChaseCamera();
		initLights();
        
		_rootNode.updateGeometricState(0, true);
		_rootNode.updateRenderState();
	}
	
	protected void initInput() {
		super.initInput();
		_input = new InputHandler();
		_clientInput = new ClientInput(_input);
		
		KeyBindingManager.getKeyBindingManager().set("gotoChat", KeyInput.KEY_SLASH);
    	_switchAction = new KeyInputAction() {
    		public void performAction(InputActionEvent evt) {
    			System.out.println("actionFired:gotoChat");
    			//_chatBox.giveFocus();
    			Main.inst().giveFocus(DefaultGuiGameState.class.getName());
    		}
    	};		
    	_input.addAction(_switchAction, "gotoChat", false);
	}
	
	/**
	 * overrides the VisualGameState initCamera in order
	 * to set the frustum far clipping pane a long ways away.
	 */
	protected void initCamera() {
		super.initCamera();
		_camera.setFrustumFar(10000);
	}
	
	protected void initChaseCamera() {
		Vector3f targetOffset = new Vector3f(0,1.0f,0);
        HashMap<String,Object> props = new HashMap<String,Object>();
        props.put(ChaseCamera.PROP_TARGETOFFSET, targetOffset);
        props.put(ChaseCamera.PROP_STAYBEHINDTARGET, true);
        props.put(ChaseCamera.PROP_MINDISTANCE, "0.1");
        props.put(ChaseCamera.PROP_MAXDISTANCE, "11");

        _chaseCamera = new ChaseCamera(_camera, _wubbleManager.getPlayerWubble().getVisualNode(), props);
        _chaseCamera.setActionSpeed(3.0f);
        _chaseCamera.setEnableSpring(false);
        
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

	    /** Set up a basic, default light. */
	    PointLight light = new PointLight();
	    light.setDiffuse( new ColorRGBA( 0.75f, 0.75f, 0.75f, 0.75f ) );
	    light.setAmbient( new ColorRGBA( 0.5f, 0.5f, 0.5f, 1.0f ) );
	    light.setLocation( new Vector3f( -8, 5, -11 ) );
	    light.setShadowCaster(true);
	    light.setEnabled( true );
	    _lightState.attach(light);

	    light = new PointLight();
	    light.setDiffuse( new ColorRGBA( 0.75f, 0.75f, 0.75f, 0.75f ) );
	    light.setAmbient( new ColorRGBA( 0.5f, 0.5f, 0.5f, 1.0f ) );
	    light.setLocation( new Vector3f( -3, 20, -80) );
	    light.setShadowCaster(true);
	    light.setEnabled( true );
	    _lightState.attach( light );
	    
        DirectionalLight dr2 = new DirectionalLight();
        dr2.setEnabled(true);
        dr2.setDiffuse(new ColorRGBA(1.0f, 1.0f, 1.0f, 1.0f));
        dr2.setAmbient(new ColorRGBA(.2f, .2f, .2f, .4f));
        dr2.setDirection(new Vector3f(-0.2f, -0.3f, -.2f).normalizeLocal());
        dr2.setShadowCaster(true);	    
        _lightState.attach( dr2 );

        _lightState.setGlobalAmbient(new ColorRGBA(0.6f, 0.6f, 0.6f, 1.0f));
        
	    /** Attach the light to a lightState and the lightState to rootNode. */
	    _rootNode.setRenderState(_lightState);
		_rootNode.updateRenderState();
	}
	
	public void setInputEnabled(boolean enabled) {
		super.setInputEnabled(enabled);
		if (enabled) 
			_input.addAction(_switchAction, "gotoChat", false);
	}
	
	public void update(float tpf) {
		super.update(tpf);
		if (!_canUpdate) 
			return;
		
		_clientInput.update(tpf);

		processUpdate();
		_wubbleManager.updateWubbles(tpf);
		Highlighter.update(tpf);

		_chaseCamera.getTarget().updateGeometricState(tpf,true);
		_chaseCamera.update(tpf);

		_rootNode.updateGeometricState(tpf, true);
	}
	
	/**
	 * 
	 */
	public float getPctDone() {
		return 1.0f;
	}

	/**
	 * 
	 */
	public void cleanup() {
		ConnectionManager.inst().disconnect();
	}
	
	/**
	 * setup connections to the server.
	 */
	public void connectToServer() {
		ConnectionManager.inst().connect(9100, 9200, this);

		short clientId = ConnectionManager.inst().getClientId();
		String name = Main.inst().getName();
		String password = Main.inst().getPassword();
		
		InvokeMessage message = InvokeMessage.createMsg("login", new Object[] {clientId,name,password});
		ConnectionManager.inst().getClient().sendToServer(message);
	}
	
	private TreeSet<String> _unknownSet = new TreeSet<String>();
	protected void processUpdate() {
		HashMap<Short,Synchronize3DMessage> msgMap = null;
		synchronized (_lock) {
			msgMap = _messageMap;
			_messageMap = new HashMap<Short,Synchronize3DMessage>();
		}
		
		for (Synchronize3DMessage msg : msgMap.values()) {
			String name = _serverMap.get(msg.getSyncObjectId());
			WubbleCharacter we = _wubbleManager.getWubble(name);
			if (we != null) {
				we.setPosition(new Vector3f(msg.getPositionX(), msg.getPositionY(), msg.getPositionZ()));
				we.setRotation(new Quaternion(msg.getRotationX(), msg.getRotationY(), 
						msg.getRotationZ(), msg.getRotationW()));
				we.updateWorldVectors();
			} else {
				if (!_unknownSet.contains(name)) {
					System.out.println("unknown spatial: " + name);
					_unknownSet.add(name);
				}
			}
		}
	}
	
	
	//--------------------------------------
	// jgn callbacks
	//--------------------------------------
	
	public void mapServerId(String name, Short id) {
		System.out.println("mapServerId: " + name + " " + id);
		_serverMap.put(id, name);
	}
	
	
	public void messageCertified(Message message) {	}
	public void messageFailed(Message message) { }
	public void messageSent(Message message) { }

	public void messageReceived(final Message message) {
		if (message instanceof InvokeMessage) {
			Callable<?> callable = new Callable<Object>() {
				public Object call() throws Exception {
					ChatRoomManager.dispatchMessage((InvokeMessage) message);
					return null;
				}
			};
			GameTaskQueueManager.getManager().update(callable);
		} else if (message instanceof Synchronize3DMessage) {
			synchronized (_lock) {
				Synchronize3DMessage msg = (Synchronize3DMessage) message;
				_messageMap.put(msg.getSyncObjectId(), msg);
			}
		} else {
//			System.out.println("Message received: " + message);
		}
	}
}
