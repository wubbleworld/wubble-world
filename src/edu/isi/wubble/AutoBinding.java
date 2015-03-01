package edu.isi.wubble;

import java.util.HashMap;

import com.jme.input.controls.Binding;

public class AutoBinding implements Binding {

	private static final long serialVersionUID = 1L;
	protected static HashMap<String,HashMap<String,AutoBinding>> _mapping;
	
	static {
		_mapping = new HashMap<String,HashMap<String,AutoBinding>>();
	}
	
	protected String _name;
	protected float _value;
	
	protected AutoBinding() { }
	
	protected AutoBinding(String name) {
		_name = name;
		_value = 0.0f;
	}
	
	public String getName() {
		return _name;
	}

	public float getValue() {
		return _value;
	}

	public void setValue(boolean status) {
		if (status) {
			_value = 1.0f;
		} else {
			_value = 0.0f;
		}
	}
	
	public static AutoBinding createBinding(String mapName, String action) {
		AutoBinding nb = new AutoBinding(action);
		if (!_mapping.containsKey(mapName)) {
			_mapping.put(mapName, new HashMap<String,AutoBinding>());
		}
		HashMap<String,AutoBinding> map = _mapping.get(mapName);
		map.put(action, nb);
		return nb;
	}
	
	public static void bindingMsg(String mapName, String action, boolean status) {
		HashMap<String,AutoBinding> map = _mapping.get(mapName);
		if (map == null) {
			System.out.println("[bindingMsg] Unknown mapping: " + mapName);
			return;
		}
		
		AutoBinding nb = map.get(action);
		if (nb == null) {
			System.out.println("[bindingMsg] Unknown action: " + action);
			return;
		}
		
		nb.setValue(status);
	}
}
