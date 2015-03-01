package edu.isi.wubble.menu;

import org.fenggui.Button;
import org.fenggui.event.ButtonPressedEvent;
import org.fenggui.event.IButtonPressedListener;
import org.fenggui.layout.StaticLayout;

import com.jme.system.DisplaySystem;

import edu.isi.wubble.Main;
import edu.isi.wubble.gamestates.GUIGameState;
import edu.isi.wubble.rpg.RPGManager;
import edu.isi.wubble.sheep.SheepLobbyState;
import edu.isi.wubble.wubbleroom.AdvancedRoomState;
import edu.isi.wubble.wubbleroom.BeginnerRoomState;
import edu.isi.wubble.wubbleroom.IntermediateRoomState;
import edu.isi.wubble.wubbleroom.RoomManager;

public class MenuGameState extends GUIGameState {

	private Button _sheepButton;
	private Button _rpgButton;

	private Button _beginnerRoomButton;
	private Button _middleRoomButton;
	private Button _advancedRoomButton;
	
	private Button _quitButton;
	
	public MenuGameState() {
		super();
		
		initGUI();
		addListeners();
	}
	
	protected void initGUI() {
		_display.setLayoutManager(new StaticLayout());
		int cWidth = DisplaySystem.getDisplaySystem().getWidth() / 2;
		int cHeight = DisplaySystem.getDisplaySystem().getHeight() / 2;
		
		_sheepButton        = makeButton("Sheep", cWidth-60, cHeight+300, 120, 30);
		_rpgButton          = makeButton("RPG", cWidth-60, cHeight+260, 120, 30);
		_beginnerRoomButton = makeButton("Beginner Room", cWidth-60, cHeight+200, 120, 30);
		_middleRoomButton   = makeButton("Intermediate Room", cWidth-60, cHeight+160, 120, 30);
		_advancedRoomButton = makeButton("Advanced Room", cWidth-60, cHeight+120, 120, 30);
		_quitButton         = makeButton("Quit", cWidth-60, cHeight+60, 120, 30);
		
		_display.addWidget(makePixmapLabel("Menu.png", 0, 0, 1024, 768));
		_display.addWidget(_sheepButton);
		_display.addWidget(_rpgButton);
		_display.addWidget(_beginnerRoomButton);
		_display.addWidget(_middleRoomButton);
		_display.addWidget(_advancedRoomButton);
		_display.addWidget(_quitButton);
		
		_display.layout();
		
	}
	
	protected void addListeners() {
		_sheepButton.addButtonPressedListener(new IButtonPressedListener() {
			public void buttonPressed(ButtonPressedEvent e) {
				Main.inst().stopState(MenuGameState.class.getName());
				Main.inst().startState(SheepLobbyState.class.getName());
			}
		});
		
		_rpgButton.addButtonPressedListener(new IButtonPressedListener() {
			public void buttonPressed(ButtonPressedEvent e) {
				Main.inst().stopState(MenuGameState.class.getName());
				RPGManager.startLogin();
			}
		});
		
		_beginnerRoomButton.addButtonPressedListener(new IButtonPressedListener() {
			public void buttonPressed(ButtonPressedEvent e) {
				RoomManager.startWubbleRoom(BeginnerRoomState.class.getName());
				Main.inst().stopState(MenuGameState.class.getName());
			}
		});
		
		_middleRoomButton.addButtonPressedListener(new IButtonPressedListener() {
			public void buttonPressed(ButtonPressedEvent e) {
				RoomManager.startWubbleRoom(IntermediateRoomState.class.getName());
				Main.inst().stopState(MenuGameState.class.getName());
			}
		});
		
		_advancedRoomButton.addButtonPressedListener(new IButtonPressedListener() {
			public void buttonPressed(ButtonPressedEvent e) {
				RoomManager.startWubbleRoom(AdvancedRoomState.class.getName());
				Main.inst().stopState(MenuGameState.class.getName());
			}
		});
		
		_quitButton.addButtonPressedListener(new IButtonPressedListener() {
			public void buttonPressed(ButtonPressedEvent e) {
				Main.inst().finish();
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
