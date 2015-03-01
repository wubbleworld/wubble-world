package edu.isi.wubble.wubbleroom;

import java.util.StringTokenizer;
import java.util.concurrent.Callable;

import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Spatial;
import com.jme.scene.shape.Sphere;
import com.jme.util.GameTaskQueueManager;
import com.jmex.physics.DynamicPhysicsNode;

import edu.isi.wubble.Main;
import edu.isi.wubble.gamestates.DefaultGuiGameState;
import edu.isi.wubble.physics.entity.DynamicEntity;
import edu.isi.wubble.physics.entity.Entity;
import edu.isi.wubble.util.Highlighter;
import edu.isi.wubble.util.LispUtils;

public abstract class RoomHandlerState extends RoomState  {

	protected String _highlighted;
	
	public RoomHandlerState() {
		super();
	}
	
	// receive messages
	// This area is for generic receive messages that apply to every room
	
	public void jump(String to) {
		System.out.println("jumping to: " + to);
		_wubble.jumpTo(LispUtils.fromLisp(to), true);
	}
	
	public void walk(String to) {
		System.out.println("moveTo: " + to);
		_wubble.moveTo(LispUtils.fromLisp(to), true);
	}
	
	public void canPickUp(String object) {
		if (_entityManager.getDynamicEntity(object) == null) {
			SocketClient.inst().sendMessage("response nil");
		} else {
			SocketClient.inst().sendMessage("response t");
		}
	}
	
	public void pickUp(String object) {
		_wubble.pickUp(object);
	}
	
	public void putDown(String ignored) {
		_wubble.putDown();
	}
	
	public void select(String object) {
		// the child already selected this object (so ignore for now).
		SocketClient.inst().sendMessage("response t");
	}
	
	public void done(String blank) {
		//Main.inst().startState(DefaultGuiGameState.class.getName());
	}
	
	public void beginJump(String blank) {
		// record the wubble's position for apple room
	}
	
	public void showSearchNode(String pos) {
		Sphere s = new Sphere("sphere", new Vector3f(0,0,0), 10, 10, 0.05f);
		s.setLocalTranslation(LispUtils.fromLisp(pos));
		s.updateWorldVectors();
		_searchNode.attachChild(s);
		_rootNode.updateRenderState();
	}
	
	public void showSearchEnd(String pos) {
		Sphere s = new Sphere("sphere", new Vector3f(0,0,0), 10, 10, 0.05f);
		s.setLocalTranslation(LispUtils.fromLisp(pos));
		s.updateWorldVectors();
		giveColor(s, ColorRGBA.white);
		_searchNode.attachChild(s);
		_rootNode.updateRenderState();
	}
	
	public void showArea(String areaDesc) {
		// format of areaDesc
		// center (x,y,z)  size (x,y,z)
		StringTokenizer str = new StringTokenizer(areaDesc, " ");
		Vector3f center = new Vector3f(Float.parseFloat(str.nextToken()),
				Float.parseFloat(str.nextToken()), Float.parseFloat(str.nextToken()));
		Vector3f size = new Vector3f(Float.parseFloat(str.nextToken()),
				Float.parseFloat(str.nextToken()), Float.parseFloat(str.nextToken()));
		
		_prepAreaSpatial.setLocalTranslation(center);
		_prepAreaSpatial.setLocalScale(size);
		_prepAreaSpatial.setCullMode(Spatial.CULL_NEVER);
	}
	
	public void showPoint(String pointDesc) {
		Vector3f center = LispUtils.fromLisp(pointDesc);
		_prepPointSpatial.setLocalTranslation(center);
		_prepPointSpatial.setCullMode(Spatial.CULL_NEVER);
	}
	
	public void clearSearchNodes(String ignore) {
		Callable<?> callable = new Callable<Object>() {
			public Object call() throws Exception {
				_searchNode.detachAllChildren();
				_rootNode.updateRenderState();
				return null;
			}
		};
		GameTaskQueueManager.getManager().update(callable);
	}
	
	public void showConcept(String concept) {
		Entity de = _entityManager.getEntity(concept);
		if (de == null) {
			System.err.println("[showConcept] unknown spatial " + concept);
			return;
		}
		System.out.println("Show concept: " + concept);
		Highlighter.createHighlighter(de.getName(), de.getNode());
		_highlighted = de.getName();
	}
	
	public void hideConcept(String concept) {
		if (_highlighted != null) {
			Highlighter.deactivateHighlighter(_highlighted);
			_highlighted = null;
		}
	}
	
	public void hidePrep(String ignore) {
		_prepAreaSpatial.setCullMode(Spatial.CULL_ALWAYS);
		_prepPointSpatial.setCullMode(Spatial.CULL_ALWAYS);
	}
	
	public void requestIdentify(String message) {
		StringTokenizer str = new StringTokenizer(message, " ");
		String adj = str.nextToken();
		String noun = str.nextToken();
		
		StringBuffer buf = new StringBuffer("What's the ");
		if (!"NIL".equals(adj) && !"nil".equals(adj)) {
			buf.append(adj + " ");
		}
		buf.append(noun + "?");
		
		RoomManager.getChat().speak(buf.toString());
		_requestInput = true;
	}
	
	public void requestPrepLocation(String message) {
		_prepSelector.setEnabled(true);
	}
}
