package edu.isi.wubble.gamestates;

import java.net.URL;

import com.jme.util.resource.ResourceLocatorTool;
import com.jme.util.resource.SimpleResourceLocator;
import com.jmex.game.state.GameState;

import edu.isi.wubble.Main;
import edu.isi.wubble.jgn.message.InvokeMessage;

public abstract class WubbleGameState extends GameState {

	public abstract void acquireFocus();
	public abstract void setInputEnabled(boolean enabled);
	public abstract float getPctDone();
	
	public WubbleGameState() {
		initState();
	}
	
	public void receivedMessage(InvokeMessage message) {
		message.callMethod(this);
	}
	
	/**
	 * initializes the state.  Sets up resource locations for derived states
	 * since all textures should be in the media/textures/ folder. 
	 *
	 */
	protected void initState() {
		try {
			URL modelURL = WubbleGameState.class.getClassLoader().getResource("media/models/");
			URL textureURL = WubbleGameState.class.getClassLoader().getResource("media/textures/");
			URL dataURL = WubbleGameState.class.getClassLoader().getResource("media/data/");
			ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_TEXTURE, 
					new SimpleResourceLocator(textureURL));
			ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_MODEL, 
					new SimpleResourceLocator(modelURL));
			ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_TEXTURE, 
					new SimpleResourceLocator(dataURL));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	
	public void setUniqueId(String uniqueId) {
		Main.inst().setId(uniqueId);
	}	
}
