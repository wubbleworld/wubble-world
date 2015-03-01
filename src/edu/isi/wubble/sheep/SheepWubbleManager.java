package edu.isi.wubble.sheep;

import java.util.HashMap;
import java.util.Set;
import java.util.logging.Logger;

import org.fenggui.util.Point;

import com.jme.renderer.ColorRGBA;
import com.jme.scene.Node;

import edu.isi.wubble.Constants;
import edu.isi.wubble.Main;
import edu.isi.wubble.WubbleManager;
import edu.isi.wubble.character.GameCharacter;
import edu.isi.wubble.character.WubbleCharacter;
import edu.isi.wubble.sheep.PowerUpInfo.PowerUpType;
import edu.isi.wubble.sheep.character.SWubble;
import edu.isi.wubble.sheep.gui.SheepGUIState;

public class SheepWubbleManager extends WubbleManager {
	private HashMap<String, GameCharacter>   _charList;
	
	// This will need to come from Main or some such place
	private Integer _myTeam = Main.inst().getTeam();
	public Integer getPlayerTeam() { return _myTeam; }
	public void    setPlayerTeam(Integer t) { _myTeam = t; }
	
	
	public GameCharacter getCharacter(String name) { 
		return _charList.get(name); 
	}
	
	public SheepWubbleManager(Node rootNode) {
		super(rootNode);
		_charList   = new HashMap<String, GameCharacter>();
	}
	
	public void addCharacter(GameCharacter g) {
		// FIXME: This stuff should be done in the GameCharacter constructor.
		_charList.put(g.getName(), g);
		_root.attachChild(g);
	}
	
	public SWubble setupPlayerWubble() {
		System.out.println("Player wubble: my team is " + getPlayerTeam());
		ColorRGBA color = Constants.COLORS[getPlayerTeam().intValue()];
		SWubble p = setupPlayerWubble(color);
		p.enablePowers();
		p.flipVisual();
		p.showName();
		
		_charList.put(Main.inst().getName(), p);
		
		SheepGUIState gui = (SheepGUIState) Main.inst().findOrCreateState(SheepGUIState.class.getName());
		gui.getMap().addWubble(Main.inst().getName(), new Point(0,0));
		
		return p;
	}
	
	public SWubble setupPlayerWubble(ColorRGBA color) {
		SWubble p = new SWubble(Main.inst().getName());
		p.setColor(color);
		
		_root.attachChild(p);
		
		_wubbleList.put(Main.inst().getName(), p);
		
		return p;
	}
	
	// Invokable
	public void addWubble(String name, ColorRGBA color, Integer team) {
		ColorRGBA actualColor = Constants.COLORS[team.intValue()];
		SWubble w = new SWubble(name);
		w.setColor(actualColor);
		w.enablePowers();
		w.flipVisual();
		w.showName();
		
		_root.attachChild(w);
		_root.updateRenderState();
		
		_wubbleList.put(name, w);
		_charList.put(name, w);
		
		System.out.println("!!! addWubble: putting " + name + " on both wubble and charlist!");
	
		if (team.equals(_myTeam)) {
			SheepGUIState gui = (SheepGUIState) Main.inst().findOrCreateState(SheepGUIState.class.getName());
			gui.getMap().addWubble(name, new Point(0,0));
		}
		
		System.out.println("Wubble Added: " + w.getName());
	} 
	
	// Invokable
	public void removeCharacter(String name) {
		GameCharacter s = _charList.remove(name);
		if (s == null) { 
			Logger.getLogger("").warning("Tried to remove non-existant sheep " + name); 
			return; 
		}
		
		s.removeFromParent();
		_root.updateRenderState();
	}

	
	public WubbleCharacter removeWubble(String name) {	
		WubbleCharacter wc = super.removeWubble(name);
		if (wc != null) {
			SheepGUIState gui = (SheepGUIState) Main.inst().findOrCreateState(SheepGUIState.class.getName());
			gui.getMap().deleteWubble(name);
		}
		System.out.println("Wubble Removed: " + name);
		return wc;
	}
	
	
	// PowerUp stuff
	/////////////////////////////////////////////////////////////////
	public void powerUpWubble(String wubName, PowerUpInfo.PowerUpType newPower) {
		WubbleCharacter w = _wubbleList.get(wubName);
		if (w != null) {
			w.powerUp(newPower);
		}
	}
	
	public void powerDownWubble(String wubName, PowerUpType pu) {
		WubbleCharacter w = _wubbleList.get(wubName);
		if (w != null) {
			w.powerDown(pu);
		}
	}
	
	public void powerDownWubbles() {
		Set<String> wubNames = _wubbleList.keySet();
		for (String name : wubNames) {
			WubbleCharacter w = _wubbleList.get(name);
			w.powerDown(PowerUpType.POWERDOWN);
		}
	}
	
	public void updateWubbles(float tpf) {
		SheepGUIState gui = (SheepGUIState) Main.inst().findOrCreateState(SheepGUIState.class.getName());
		
		for (WubbleCharacter w : _wubbleList.values()) {
			w.update(tpf);
			gui.getMap().updateWubble(w.getName(), w.getLocalTranslation(), w.getLocalRotation());
		}
	}
}
