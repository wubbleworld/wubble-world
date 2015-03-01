package edu.isi.wubble;

import java.util.HashMap;
import java.util.concurrent.Callable;

import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Node;
import com.jme.util.GameTaskQueueManager;

import edu.isi.wubble.Main;
import edu.isi.wubble.character.WubbleCharacter;

public class WubbleManager {
	public class WubbleRemover implements Callable<Object> {
		WubbleCharacter __deadWubble;
		public WubbleRemover(WubbleCharacter w) {
			__deadWubble = w;
		}
		
		public Object call() throws Exception {
			__deadWubble.removeFromParent();
			return new Object();
		}
	}
	
	public Node _root;
	protected HashMap<String, WubbleCharacter> _wubbleList;
	
	public WubbleManager() {
		_wubbleList = new HashMap<String, WubbleCharacter>();
	}
	
	public WubbleManager(Node rootNode) {
		this();
		_root = rootNode;
	}
	
	public void setRootNode(Node rootNode) {
		_root = rootNode;
	}
	
	public WubbleCharacter setupPlayerWubble(ColorRGBA color) {
		WubbleCharacter p = new WubbleCharacter(Main.inst().getName());
		p.setColor(color);
		
		_root.attachChild(p);
		
		_wubbleList.put(Main.inst().getName(), p);
		
		return p;
	}
	
	public WubbleCharacter getPlayerWubble() {
		return _wubbleList.get(Main.inst().getName());
	}
	
	// Invokable
	public WubbleCharacter addWubble(String name, ColorRGBA color) {
		WubbleCharacter w = _wubbleList.get(name);
		if (w == null) {
			w = new WubbleCharacter(name);
		} else {
			System.out.println("Wubble collision: " + name);
		}
		w.setColor(color);
		
		_root.attachChild(w);
		_root.updateRenderState();
		
		_wubbleList.put(name, w);
		System.out.println("Wubble Added: " + w.getName());
		
		return w;
	} 
	
	public WubbleCharacter addWubble(String name, Vector3f pos, Quaternion rot, ColorRGBA color) {
		WubbleCharacter w = addWubble(name, color);
		w.setPosition(pos);
		w.setRotation(rot);
		return w;
	}
	
	// Invokable
	public WubbleCharacter removeWubble(String name) {	
		WubbleCharacter wc = _wubbleList.remove(name);
		if (wc != null) {
			WubbleRemover wr = new WubbleRemover(wc);
			GameTaskQueueManager.getManager().update(wr);
		}
		return wc;
	}

	public WubbleCharacter getWubble(String wubName) {
		return _wubbleList.get(wubName);
	}

	public void updateWubbles(float tpf) {
		for (WubbleCharacter w : _wubbleList.values()) {
			w.update(tpf);
		}
	}
}
