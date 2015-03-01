package edu.isi.wubble.rpg;

import static edu.isi.wubble.jgn.rpg.RPGPhysics.SHOOTER;
import static edu.isi.wubble.jgn.rpg.RPGPhysics.PICKER;

import static com.jme.input.InputHandler.AXIS_NONE;
import static com.jme.input.InputHandler.BUTTON_ALL;
import static com.jme.input.InputHandler.DEVICE_MOUSE;
import static com.jme.util.resource.ResourceLocatorTool.TYPE_MODEL;

import java.awt.Font;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import jmetest.effects.RenParticleEditor;

import com.captiveimagination.jgn.event.MessageListener;
import com.captiveimagination.jgn.message.Message;
import com.jme.animation.SpatialTransformer;
import com.jme.bounding.BoundingBox;
import com.jme.image.Texture;
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
import com.jme.scene.CameraNode;
import com.jme.scene.Controller;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jme.scene.TriMesh;
import com.jme.scene.state.AlphaState;
import com.jme.scene.state.CullState;
import com.jme.scene.state.FogState;
import com.jme.scene.state.LightState;
import com.jme.scene.state.RenderState;
import com.jme.scene.state.TextureState;
import com.jme.system.DisplaySystem;
import com.jme.util.GameTaskQueueManager;
import com.jme.util.TextureManager;
import com.jme.util.export.binary.BinaryImporter;
import com.jme.util.resource.ResourceLocatorTool;
import com.jmex.effects.particles.ParticleFactory;
import com.jmex.effects.particles.ParticleGeometry;
import com.jmex.effects.particles.ParticleInfluence;
import com.jmex.effects.particles.ParticleMesh;
import com.jmex.effects.particles.SimpleParticleInfluenceFactory;
import com.jmex.font3d.Font3D;
import com.jmex.font3d.Text3D;
import com.jmex.font3d.effects.Font3DGradient;
import com.jmex.game.state.GameStateManager;

import edu.isi.wubble.Main;
import edu.isi.wubble.character.WubbleCharacter;
import edu.isi.wubble.character.WubbleCrossbowCharacter;
import edu.isi.wubble.gamestates.DefaultGuiGameState;
import edu.isi.wubble.gamestates.VisualGameState;
import edu.isi.wubble.jgn.ConnectionManager;
import edu.isi.wubble.jgn.message.HiddenUpdateMessage;
import edu.isi.wubble.jgn.message.InvokeMessage;
import edu.isi.wubble.jgn.message.WorldUpdateMessage;
import edu.isi.wubble.util.Highlighter;
import edu.isi.wubble.util.PickerI;
import edu.isi.wubble.util.PickerInputAction;

public class RPGWinningState extends VisualGameState {

	protected KeyInputAction _switchAction;
	protected LightState _lightState;
	protected FogState _fogState;
	
	protected Spatial _shooter;
	protected Spatial _picker;
	
	protected WubbleCharacter _shooterWubble;
	protected WubbleCharacter _pickerWubble;

	protected SpatialTransformer _transformer;

	protected Font3D _font;

	public RPGWinningState() {
		super();
		
		init();
		setActive(false);
	}
	
	protected void init() {
		//_showBounding = true;
		initLights();
		initWorld();
				
		_transformer = new SpatialTransformer(1);
		
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
		
        _font = new Font3D(new Font("Arial", Font.PLAIN, 2), 0.1, true, true, true);
        Font3DGradient gradient = new Font3DGradient(new Vector3f(0,-1,0), ColorRGBA.lightGray, ColorRGBA.black);
        gradient.applyEffect(_font);
        
        Text3D mytext = _font.createText("You Win!!!", 1, 0);
        mytext.setLocalScale(new Vector3f(0.5f,0.5f,0.1f));
        mytext.setLocalTranslation(new Vector3f(-11.7f, 12, -84));
        _rootNode.attachChild(mytext);
        
        _rootNode.updateGeometricState(0, true);
		_rootNode.updateRenderState();
	}
	
	/**
	 *
	 */
	protected void initWorld() {
		try {
			URL url = ResourceLocatorTool.locateResource(TYPE_MODEL, "rpg-winning.jme");
			Node levelNode = (Node) BinaryImporter.getInstance().load(url);
			List<Spatial> children = levelNode.getChildren();
			while (children.size() > 0) {
				Spatial s = children.remove(0);
				if (s.getName().startsWith("particleEmitter")) {
					buildParticleSystem(s);
				} else if ("shooterWubble".equals(s.getName())) {
					_shooter = s;
					_shooter.getLocalTranslation().addLocal(new Vector3f(0,0.2f,0));
				} else if ("pickerWubble".equals(s.getName())) {
					_picker = s;
					_picker.getLocalTranslation().addLocal(new Vector3f(0,0.2f,0));
				} else {
					_rootNode.attachChild(s);
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	protected void buildParticleSystem(Spatial s) {
	    ParticleGeometry particleGeom = ParticleFactory.buildParticles(s.getName(), 700);

        particleGeom.addInfluence(SimpleParticleInfluenceFactory
                .createBasicGravity(new Vector3f(0, -1.5f, 0), true));
        particleGeom.setEmissionDirection(new Vector3f(0.0f, 1.0f, 0.0f));
        particleGeom.setMaximumAngle(0.2268928f);
        particleGeom.setMinimumAngle(0);
        particleGeom.getParticleController().setSpeed(0.1f);
        particleGeom.setMinimumLifeTime(1300.0f);
        particleGeom.setMaximumLifeTime(1950.0f);
        particleGeom.setStartSize(0.15f);
        particleGeom.setEndSize(0.15f);
        particleGeom
                .setStartColor(new ColorRGBA(0.0f, 0.0625f, 1.0f, 1.0f));
        particleGeom.setEndColor(new ColorRGBA(0.0f, 0.0625f, 1.0f, 0.0f));
        particleGeom.getParticleController().setControlFlow(false);
        particleGeom.setReleaseRate(500);
        particleGeom.setReleaseVariance(0.0f);
        particleGeom.setInitialVelocity(0.1f);
        particleGeom.getParticleController().setRepeatType(
                Controller.RT_WRAP);	

        AlphaState as = (AlphaState) particleGeom.getRenderState(RenderState.RS_ALPHA);
        if (as == null) {
        	as = DisplaySystem.getDisplaySystem().getRenderer().createAlphaState();
        	as.setBlendEnabled(true);
        	as.setSrcFunction(AlphaState.SB_SRC_ALPHA);
        	as.setTestEnabled(true);
        	as.setTestFunction(AlphaState.TF_GREATER);
        	particleGeom.setRenderState(as);
        	particleGeom.updateRenderState();
        }
        as.setDstFunction(AlphaState.DB_ONE);
        
        TextureState ts = DisplaySystem.getDisplaySystem().getRenderer().createTextureState();
        ts.setTexture(TextureManager.loadTexture(RenParticleEditor.class
                .getClassLoader().getResource(
                        "jmetest/data/texture/flaresmall.jpg"),
                Texture.FM_LINEAR, Texture.FM_LINEAR));
        particleGeom.setRenderState(ts);
        
	    Node particleNode = new Node();
	    particleNode.setLocalTranslation(new Vector3f(s.getLocalTranslation()));
	    particleNode.attachChild(particleGeom);
	    
	    _rootNode.attachChild(particleNode);
	}
	
	
	public void setWubbles(WubbleCharacter shooter, WubbleCharacter picker) {
		shooter.setPosition(new Vector3f(_shooter.getLocalTranslation()));
		shooter.getVisualNode().setLocalRotation(new Quaternion());
		shooter.playAnimation("idle");
		_shooterWubble = shooter;
		_rootNode.attachChild(shooter);

		picker.setPosition(new Vector3f(_picker.getLocalTranslation()));
		picker.getVisualNode().setLocalRotation(new Quaternion());
		picker.playAnimation("idle");
		_pickerWubble = picker;
		_rootNode.attachChild(picker);
	}
	
	public void addEntities(HashMap<String,Spatial> entityMap) {
		for (Spatial s : entityMap.values()) {
			if (!s.getName().startsWith("stitch")) {
				_rootNode.attachChild(s);
			}
		}
	}
	
	public void activateState() {
		//_transformer.setRepeatType(Controller.RT_CLAMP);
		//_transformer.setPosition(0, 10, new Vector3f(-11f,10.5f,-85f));
		//_transformer.setRotation(0, 10, new Quaternion());
		//_transformer.setObject(_cameraNode, 0, -1);
		
		_camera = DisplaySystem.getDisplaySystem().getRenderer().getCamera();
		_camera.setLocation(new Vector3f(-10.5f,11f,-80f));
		_camera.lookAt(new Vector3f(-10.5f, 11f, -81f), new Vector3f(0,1,0));
		_camera.update();
		
		_rootNode.updateRenderState();
		setActive(true);
	}
	
	protected void initInput() {
		//super.initInput();
		_input = new InputHandler();
		
        KeyBindingManager.getKeyBindingManager().set("gotoChat", KeyInput.KEY_SLASH);
    	_switchAction = new KeyInputAction() {
    		public void performAction(InputActionEvent evt) {
    			System.out.println("actionFired:gotoChat");
    			Callable<?> callable = new Callable<Object>() {
					public Object call() throws Exception {
		    			Main.inst().giveFocus(DefaultGuiGameState.class.getName());
						return null;
					}
    			};
    			GameTaskQueueManager.getManager().update(callable);
    		}
    	};		
    	_input.addAction(_switchAction, "gotoChat", false);
	}
	
	/**
	 * overrides the VisualGameState initCamera in order
	 * to set the frustum far clipping pane a long ways away.
	 */
	protected void initCamera() {
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
	    light.setLocation( new Vector3f( 7.5f, 5, 7.5f ) );
	    light.setEnabled( true );
	    _lightState.attach(light);

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

		_shooterWubble.update(tpf);
		_pickerWubble.update(tpf);
				
		//_transformer.update(tpf);
		_rootNode.updateGeometricState(tpf, true);
	}
	
	public void render(float tpf) {
		super.render(tpf);
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
	}
}
