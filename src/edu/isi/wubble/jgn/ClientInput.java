package edu.isi.wubble.jgn;

import java.util.*;

import com.jme.input.*;
import com.jme.input.action.*;

import edu.isi.wubble.jgn.message.InvokeMessage;

public class ClientInput {
	
	protected float velocity = 4.0f;
	
	protected InputHandler _input;
	protected String _wubbleRole;
	
	protected TreeMap<String,Boolean> _wasDownMap;
	protected TreeMap<String,Boolean> _isDownMap;
	
	
	public ClientInput(InputHandler input) {
		_input = input;

		_wasDownMap = new TreeMap<String,Boolean>();
		_wasDownMap.put("forward", false);
		_wasDownMap.put("backward", false);
		_wasDownMap.put("left", false);
		_wasDownMap.put("right", false);
		_wasDownMap.put("jump", false);

		_isDownMap = new TreeMap<String,Boolean>();
		_isDownMap.put("forward", false);
		_isDownMap.put("backward", false);
		_isDownMap.put("left", false);
		_isDownMap.put("right", false);
		_isDownMap.put("jump", false);
		
		init();
	}
	
	public void setWubbleRole(String name) {
		_wubbleRole = name;
	}
	
	protected void init() {
		setKeyBindings();
		setActions();
	}

    /**
     * creates the keyboard object, allowing us to obtain the values of a keyboard as keys are
     * pressed. It then sets the actions to be triggered based on if certain keys are pressed (WSAD).
     */
    protected void setKeyBindings() {
        KeyBindingManager keyboard = KeyBindingManager.getKeyBindingManager();
        keyboard.remove("toggle_wire");
        keyboard.remove("toggle_pause");
        keyboard.remove("toggle_lights");
        keyboard.remove("toggle_bounds");
        keyboard.remove("toggle_normals");
        keyboard.remove("camera_out");
        keyboard.remove("mem_report");
        
        keyboard.set("forward", KeyInput.KEY_W);
        keyboard.set("backward", KeyInput.KEY_S);
        keyboard.set("right", KeyInput.KEY_D);
        keyboard.set("left", KeyInput.KEY_A);
        keyboard.set("jump", KeyInput.KEY_SPACE);
    }
    
    /**
     * assigns action classes to triggers. These actions handle moving the node forward, backward and 
     * rotating it.
     * @param node the node to control.
     */
    private void setActions() {
    	InputAction pressedAction = new InputAction() {
    		public void performAction(InputActionEvent evt) {
    			_isDownMap.put(evt.getTriggerName(), new Boolean(true));
    		}
    	};
    	
    	_input.addAction(pressedAction, "forward", true);
    	_input.addAction(pressedAction, "backward", true);
    	_input.addAction(pressedAction, "left", true);
    	_input.addAction(pressedAction, "right", true);
    	_input.addAction(pressedAction, "jump", true);
    }
	
    
    public void update(float tpf) {
    	Iterator<String> iter = _isDownMap.keySet().iterator();
    	while (iter.hasNext()) {
    		String action = iter.next();
    		if (_wasDownMap.get(action).booleanValue() && !_isDownMap.get(action).booleanValue()) {
    			InvokeMessage mm = InvokeMessage.createMsg("movementMsg", new Object[] {
    					ConnectionManager.inst().getClientId(), 
    					action, false 
    			});
    			mm.sendToServer();
    		}
    		
    		if (_isDownMap.get(action).booleanValue() && !_wasDownMap.get(action).booleanValue()) {
    			InvokeMessage mm = InvokeMessage.createMsg("movementMsg", new Object[] {
    					ConnectionManager.inst().getClientId(), 
    					action, true
    			});
    			mm.sendToServer();
    		}
    		
    		_wasDownMap.put(action, _isDownMap.get(action));
    		_isDownMap.put(action, new Boolean(false));
    	}
    }
}
