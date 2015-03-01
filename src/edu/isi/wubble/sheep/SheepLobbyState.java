package edu.isi.wubble.sheep;

import org.fenggui.Button;
import org.fenggui.background.PlainBackground;
import org.fenggui.event.ButtonPressedEvent;
import org.fenggui.event.IButtonPressedListener;
import org.fenggui.util.Color;
import org.fenggui.util.Point;

import edu.isi.wubble.Constants;
import edu.isi.wubble.Main;
import edu.isi.wubble.gamestates.GUIGameState;
import edu.isi.wubble.sheep.gui.SheepGUIState;

public class SheepLobbyState extends GUIGameState {
	Button _joinRed, _joinBlue;
	Point _center;
	
	public SheepLobbyState() {
		super();
		
		initGUI();
		addListeners();
	}
	
	private void initGUI() {
		_center = new Point(_display.getWidth() / 2, _display.getHeight() / 2);
		
		System.out.println(_center);
		
		_joinRed = makeButton("Join Red Team", _center.getX() + 40, _center.getY(), 200, 40);
		_joinBlue = makeButton("Join Blue Team", _center.getX() - 50 - 200, _center.getY(), 200, 40);

		_display.addWidget(_joinRed);
		_display.addWidget(_joinBlue);
		
		_display.layout();
	}

	private void addListeners() {
		_joinBlue.addButtonPressedListener(new IButtonPressedListener() {
			public void buttonPressed(ButtonPressedEvent e) {
				System.out.println("HERE");
				Main.inst().setTeam(Constants.BLUE_TEAM);
				Main.inst().stopState(SheepLobbyState.class.getName());
				System.out.println("thread: " + Thread.currentThread().getName());
				Main.inst().startState(SheepGameState.class.getName());
				Main.inst().startState(SheepGUIState.class.getName());
			}
		});
		
		_joinRed.addButtonPressedListener(new IButtonPressedListener() {
			public void buttonPressed(ButtonPressedEvent e) {
				System.out.println("HERE");
				Main.inst().setTeam(Constants.RED_TEAM);
				Main.inst().stopState(SheepLobbyState.class.getName());
				System.out.println("thread: " + Thread.currentThread().getName());
				Main.inst().startState(SheepGameState.class.getName());
				Main.inst().startState(SheepGUIState.class.getName());
			}
		});
	}
	
	@Override
	public void acquireFocus() {
	}

	@Override
	public float getPctDone() {
		return 1.0f;
	}

}
