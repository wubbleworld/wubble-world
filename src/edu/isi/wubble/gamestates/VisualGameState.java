package edu.isi.wubble.gamestates;

import java.util.concurrent.Callable;

import com.jme.bounding.BoundingBox;
import com.jme.input.FirstPersonHandler;
import com.jme.input.InputHandler;
import com.jme.input.KeyBindingManager;
import com.jme.input.KeyInput;
import com.jme.input.action.InputActionEvent;
import com.jme.input.action.KeyInputAction;
import com.jme.renderer.Camera;
import com.jme.renderer.Renderer;
import com.jme.scene.Node;
import com.jme.scene.SceneElement;
import com.jme.scene.Text;
import com.jme.scene.state.RenderState;
import com.jme.scene.state.TextureState;
import com.jme.scene.state.ZBufferState;
import com.jme.system.DisplaySystem;
import com.jme.util.GameTaskQueueManager;
import com.jme.util.Timer;
import com.jme.util.geom.Debugger;
import com.jmex.game.state.GameState;

import edu.isi.wubble.Main;
import edu.isi.wubble.rpg.RpgGuiState;

public abstract class VisualGameState extends WubbleGameState {
	protected Camera       _camera;
	public    Camera       getCamera() { return _camera; }
	
	protected Node         _rootNode;
	protected InputHandler _input;
	public InputHandler getInput() { return _input; }
	
	protected Timer        _timer;
    protected StringBuffer _updateBuffer = new StringBuffer( 30 );
    protected StringBuffer _tempBuffer = new StringBuffer();
    protected Node         _fpsNode;
    protected Text         _fps;
    protected boolean      _renderStatistics = true;
 
    protected boolean      _showBounding     = false;
    protected boolean      _delayInputUpdate = false;
    
	protected KeyInputAction _switchAction;    
	
	public VisualGameState() {
		super();
		
		_rootNode = new Node("rootNode");

		DisplaySystem.getDisplaySystem().getRenderer().enableStatistics(true);
		ZBufferState buf = DisplaySystem.getDisplaySystem().getRenderer().createZBufferState();
        buf.setEnabled(true);
        buf.setFunction(ZBufferState.CF_LEQUAL);
        _rootNode.setRenderState(buf);
 
        // Then our font Text object.
        /** This is what will actually have the text at the bottom. */
        _fps = Text.createDefaultTextLabel( "FPS label" );
        _fps.setCullMode( SceneElement.CULL_NEVER );
        _fps.setTextureCombineMode( TextureState.REPLACE );

        // Finally, a stand alone node (not attached to root on purpose)
        _fpsNode = new Node( "FPS node" );
        _fpsNode.setRenderState( _fps.getRenderState( RenderState.RS_ALPHA ) );
        _fpsNode.setRenderState( _fps.getRenderState( RenderState.RS_TEXTURE ) );
        _fpsNode.attachChild( _fps );
        _fpsNode.setCullMode( SceneElement.CULL_NEVER );
        
        initCamera();
        initInput();
        
        _timer = Timer.getTimer();
        
        // Update geometric and rendering information for the rootNode.
        _rootNode.updateGeometricState(0.0f, true);
        _rootNode.updateRenderState();

        _fpsNode.updateGeometricState( 0.0f, true );
        _fpsNode.updateRenderState();

	}

	/**
	 * initializes our input to a first person handler (to make my life easier).
	 *
	 */
	protected void initInput() {
		_input = new FirstPersonHandler(_camera, 3.0f, 3.0f);
	}
	
	/**
	 * Initializes a standard camera.
	 */
	protected void initCamera() {
		DisplaySystem display = DisplaySystem.getDisplaySystem();
		
		_camera = display.getRenderer().createCamera(display.getWidth(), display.getHeight());
		_camera.setFrustumPerspective(45.0f, 
				(float) display.getWidth() / (float) display.getHeight(), 1, 1000);
		
		_camera.update();
	}
	

	/**
	 * turns on and off the input handler
	 * @param enabled
	 * 			status of the input handler
	 */
	public void setInputEnabled(boolean enabled) {
		_input.setEnabled(enabled);
		
		if (enabled && _switchAction != null) 
			_input.addAction(_switchAction, "gotoChat", false);
	}

	/**
	 * Draws the rootNode.
	 * 
	 * @see GameState#render(float)
	 */
	public void render(float tpf) {
		DisplaySystem.getDisplaySystem().getRenderer().draw(_rootNode);
		if (_renderStatistics) {
			DisplaySystem.getDisplaySystem().getRenderer().draw(_fpsNode);
		}
		
		if (_showBounding) {
			boolean showChildren = true;
			Debugger.drawBounds(_rootNode, DisplaySystem.getDisplaySystem()
					.getRenderer(), showChildren);
		}
	}

	/**
	 * Updates the rootNode.
	 * 
	 * @see GameState#update(float)
	 */
	public void update(float tpf) {
		if (!_delayInputUpdate) {
			_input.update(tpf);
		}

		if (_renderStatistics) {
			Renderer renderer = DisplaySystem.getDisplaySystem().getRenderer();
			setUpdateBuffer();
			/** Send the fps to our fps bar at the bottom. */
			_fps.print( _updateBuffer );
			renderer.clearStatistics();
		} 
		// we need to call this as little as possible, but it
		// needs to be called at least once.  Preferably completely at
		// the end of the update, so make sure you do it in your function
		//_rootNode.updateGeometricState(tpf, true);
	}
	
	
	// Set the text for the FPS status line at the bottom of the screen. 
	public void setUpdateBuffer() {
		Renderer renderer = DisplaySystem.getDisplaySystem().getRenderer();
		_updateBuffer.setLength(0);
		_updateBuffer.append( "FPS: " ).append( (int) _timer.getFrameRate() ).append(
					" - " );
		_updateBuffer.append( renderer.getStatistics( _tempBuffer ) );	
	}
	
	
	/**
	 * defer updating the input until a later time.
	 * necessary for Physics since they use the InputHandler
	 * to pass collision events.
	 * @param enabled
	 */
	protected void setDelayInputUpdate(boolean enabled) {
		_delayInputUpdate = enabled;
	}
	
    /**
	 * Overwritten to appropriately call switchTo() or switchFrom().
	 *
	 * @see GameState#setActive(boolean)
	 */
	public void setActive(boolean active) {
		if (active) onActivate();
		super.setActive(active);
	}
	
    /**
	 * Points the renderers camera to the one contained by this state. Derived 
	 * classes can put special actions they want to perform when activated here.
	 */
	protected void onActivate() {
		DisplaySystem.getDisplaySystem().getRenderer().setCamera(_camera);
	}
	
	/**
	 * 
	 */
	public void acquireFocus() {
		// since we are visual.... don't do anything
	}
	
	public void addScreenShot(int key) {
        KeyBindingManager.getKeyBindingManager().set("screenshot", key);
        KeyInputAction screeny = new KeyInputAction() {
        	public void performAction(InputActionEvent evt) {
                DisplaySystem.getDisplaySystem().getRenderer().takeScreenShot( "ScreenShot" );
        	}
        };
        _input.addAction(screeny, "screenshot", false);
	}

	public void addChatSwitchAbility(final String className) {
    	KeyBindingManager.getKeyBindingManager().set("gotoChat", KeyInput.KEY_SLASH);
    	_switchAction = new KeyInputAction() {
    		public void performAction(InputActionEvent evt) {
    			System.out.println("actionFired:gotoChat " + className);
    			Callable<?> callable = new Callable<Object>() {
					public Object call() throws Exception {
		    			Main.inst().giveFocus(className);
						return null;
					}
    			};
    			GameTaskQueueManager.getManager().update(callable);
    		}
    	};		
    	_input.addAction(_switchAction, "gotoChat", false);
	}
}
