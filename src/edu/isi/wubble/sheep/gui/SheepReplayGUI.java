package edu.isi.wubble.sheep.gui;


import org.fenggui.Button;
import org.fenggui.event.ButtonPressedEvent;
import org.fenggui.event.IButtonPressedListener;
import org.fenggui.layout.StaticLayout;

import com.jme.system.DisplaySystem;

import edu.isi.wubble.gamestates.GUIGameState;
import edu.isi.wubble.jgn.sheep.SheepPhysicsState;
import edu.isi.wubble.jgn.sheep.Utils;
import edu.isi.wubble.sheep.Replayer;

public class SheepReplayGUI extends GUIGameState {

	private Button _replayButton;
	private Button _pauseButton;
	private Button _stepFwdButton;
	private Button _stepBackButton;
	
	SheepPhysicsState _sps;
	public SheepReplayGUI() {
		super();
		
		_sps = Utils.GetSps();
		System.out.println("Creating Sheep replay GUI.");
		initGUI();
		addListeners();
	}
	
	protected void initGUI() {
		_display.setLayoutManager(new StaticLayout());
		int cWidth = DisplaySystem.getDisplaySystem().getWidth() / 4;
		int cHeight = DisplaySystem.getDisplaySystem().getHeight() / 4;
		
		
		_replayButton        = makeButton("Replay", cWidth-60, cHeight+100, 120, 30);
		_pauseButton          = makeButton("Pause", cWidth-60, cHeight+60, 120, 30);
		_stepFwdButton = makeButton("Step fwd", cWidth-60, cHeight, 120, 30);
		_stepBackButton   = makeButton("Step back", cWidth-60, cHeight-40, 120, 30);
//		_advancedRoomButton = makeButton("Advanced Room", cWidth-60, cHeight-80, 120, 30);
//		_quitButton         = makeButton("Quit", cWidth-60, cHeight-120, 120, 30);
		
		_display.addWidget(_replayButton);
		_display.addWidget(_pauseButton);
		_display.addWidget(_stepFwdButton);
		_display.addWidget(_stepBackButton);
//		_display.addWidget(_advancedRoomButton);
//		_display.addWidget(_quitButton);
		
		_display.layout();
		
		// See what happens here.
//		Window window = FengGUI.createDialog(_display);
//    	window.setX(100);
//    	window.setY(50);
//    	
//    	window.setSize(200, 100);
//    	window.setTitle("FengGUI says...");
		
	}
	
	protected void addListeners() {



		// Toggle the state of the replay.
		_pauseButton.addButtonPressedListener(new IButtonPressedListener() {
			public void buttonPressed(ButtonPressedEvent e) {
				Replayer rp = _sps.getReplayer();
				rp.pauseButton();
			}
		});
		
		// Replay from the beginning.
		_replayButton.addButtonPressedListener(new IButtonPressedListener() {
			public void buttonPressed(ButtonPressedEvent e) {
				Replayer rp = _sps.getReplayer();
				// Should reset the world to be in accordance with the replay - delete all dynamic entities.
				System.out.println("Resuming replay from the beginning.");
				rp.replayButton();
			}
		});
		
		_stepFwdButton.addButtonPressedListener(new IButtonPressedListener() {
			public void buttonPressed(ButtonPressedEvent e) {
				System.out.println("Stepping forward!");
			}
		});
		
		_stepBackButton.addButtonPressedListener(new IButtonPressedListener() {
			public void buttonPressed(ButtonPressedEvent e) {
				System.out.println("Stepping back!");
			}
		});
		
	}
	
	public void acquireFocus() {
		
	}

	/**
	 * 
	 */
	public float getPctDone() {
		return 1.0f;
	}
}
