package edu.isi.wubble.util;

import java.util.*;

import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jme.scene.state.*;
import com.jme.renderer.*;

public class Highlighter {

	public static ColorRGBA DELTA = new ColorRGBA(0.025f,0.025f,0.025f,0);;
	
	protected static HashMap<String,Highlighter> _highlighterMap = 
		new HashMap<String,Highlighter>();
	protected static Object _lock = new Object();
	
	protected String _name;
	protected ArrayList<Spatial> _spatials;
	protected ArrayList<MaterialState> _materials;
	protected ArrayList<ColorRGBA> _originals;
	
	protected int _direction = 1;
	
	protected Highlighter(String name, Spatial s) {
		_name = name;
		_spatials = new ArrayList<Spatial>();
		_materials = new ArrayList<MaterialState>();
		_originals = new ArrayList<ColorRGBA>();
		
		walkNode(s);
	}
	
	/*
	 * walk each node looking for spatials with material properties
	 * so that they can be added to the highlighter.
	 */
	protected void walkNode(Spatial s) {
		if (s.getRenderState(RenderState.RS_MATERIAL) != null) {
			addMaterial(s);
		}
		
		try {
			Node n = (Node) s;
			Iterator<Spatial> children = n.getChildren().iterator();
			while (children.hasNext()) {
				walkNode(children.next());
			}
		} catch (Exception e) { }
	}
	
	/*
	 * add a list to the maintenance list so that we can select good stuff.
	 */
	protected void addMaterial(Spatial s) {
		MaterialState ms = (MaterialState) s.getRenderState(RenderState.RS_MATERIAL);
		
		_spatials.add(s);
		_materials.add((MaterialState) s.getRenderState(RenderState.RS_MATERIAL));
		_originals.add(new ColorRGBA(ms.getAmbient()));
		
		ms.setAmbient(new ColorRGBA(0,0,0,0));
	}
	
	protected void deactivate() {
		for (int i = 0; i < _spatials.size(); ++i) {
			Spatial s = _spatials.get(i);
			MaterialState ms = _materials.get(i);
			ColorRGBA original = _originals.get(i);
			
			ms.setAmbient(original);
			s.updateRenderState();
		}
	}
	
	/*
	 * no physics running so updates must be called manually
	 */
	public static void createHighlighter(String name, Spatial s) {
		if (_highlighterMap.containsKey(name)) {
			System.err.println("****** Highlighter collision: " + name);
		}

		Highlighter h = new Highlighter(name, s);
		synchronized (_lock) {
			_highlighterMap.put(name, h);
		}
	}
	
	public static void deactivateHighlighter(String name) {
		Highlighter h = null;
		synchronized (_lock) {
			h = _highlighterMap.remove(name);
		}
		if (h != null) {
			h.deactivate();
		}
	}
	
	public static void deactivateAll() {
		synchronized (_lock) {
			for (Highlighter h : _highlighterMap.values()) {
				h.deactivate();
			}
			_highlighterMap.clear();
		}
	}
	
	/*
	 * only call when highlighters not created with
	 * PhysicsSpaces.
	 */
	public static void update(float tpf) {
		synchronized (_lock) {
			for (Highlighter h : _highlighterMap.values()) {
				h.updateInternal(tpf);
			}
		}
	}

	protected void updateInternal(float tpf) {
		for (int i = 0; i < _spatials.size(); ++i) {
			Spatial s = _spatials.get(i);
			MaterialState ms = _materials.get(i);

			ColorRGBA ambient = ms.getAmbient();
			ambient = ambient.add(DELTA.clone().multLocal(_direction));

			if (ambient.r >= 1.0f) {
				_direction = -1;
			} 
		
			if (ambient.r <= 0.0f) {
				_direction = 1;
			}
			ms.setAmbient(ambient);
			s.updateRenderState();
		}
	}
}
