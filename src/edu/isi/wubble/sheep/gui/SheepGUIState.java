package edu.isi.wubble.sheep.gui;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.Callable;

import javax.imageio.ImageIO;

import org.fenggui.Label;
import org.fenggui.background.PixmapBackground;
import org.fenggui.composites.Window;
import org.fenggui.event.ButtonPressedEvent;
import org.fenggui.event.IButtonPressedListener;
import org.fenggui.event.IKeyReleasedListener;
import org.fenggui.event.Key;
import org.fenggui.event.KeyReleasedEvent;
import org.fenggui.layout.Alignment;
import org.fenggui.render.Binding;
import org.fenggui.render.Pixmap;
import org.fenggui.text.TextStyle;
import org.fenggui.util.Color;
import org.fenggui.util.Point;

import com.jme.input.KeyInput;
import com.jme.system.DisplaySystem;
import com.jme.util.GameTaskQueueManager;
import com.jme.util.resource.ResourceLocatorTool;

import edu.isi.wubble.Constants;
import edu.isi.wubble.Main;
import edu.isi.wubble.gamestates.DefaultGuiGameState;
import edu.isi.wubble.menu.MenuGameState;
import edu.isi.wubble.rpg.RPGManager;
import edu.isi.wubble.sheep.SheepGameState;

public class SheepGUIState extends DefaultGuiGameState {
//	TextArea _chatLog;
//	TextEditor _chatBox;
	
	TextStyle _redStyle, _blueStyle;
	
	Label _shiftLabel, _blueScoreLabel, _redScoreLabel;
	Map _map;
	
	public SheepGUIState() {
		super();
		
		initSheepGUI();
		
		setupHelpScreen();
		
		_display.layout();
	}
	
	private void initSheepGUI() {
		_redStyle = new TextStyle(_fengArialBold, Color.RED);
		_blueStyle = new TextStyle(_fengArialBold, Color.BLUE);
//		
		_textArea.setText("");
		systemMessage("Welcome to Wubble World's new game Sheep!");
		systemMessage("Voice Chat by holding down the SHIFT key!");
		systemMessage("Press / (Slash) to type a message below...");
		systemMessage("then press ENTER to send it and return to the action!");
		// we can use this to always go to last line when a message is received 
		_textArea.scrollVertical(0.0);
		
		_textEditor.addKeyReleasedListener(new IKeyReleasedListener() {
			public void keyReleased(KeyReleasedEvent evt) {
				int key = (int) evt.getKey();
				
				if ( key == KeyInput.KEY_SLASH ) {
					System.out.println("HERE");
					
					setInputEnabled(false);

					_display.fireKeyPressedEvent((char) 0, Key.BACKSPACE); // hehe hackery!
					
					_display.setFocusedWidget(null);
					Main.inst().giveFocus(SheepGameState.class.getName());
				}
			}
		});
		
		// This is the map
		try {
			_map = new Map();
			_map.setPosition(new Point( _display.getWidth() - 250  , _display.getHeight() - 430 ));
			
			_display.addWidget(_map);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// This is the "Press Shift" label
		_shiftLabel = new Label("Press SHIFT to Talk");
		_shiftLabel.setSize(512, 32);
		_shiftLabel.setPosition(new Point(40, 170));
		_shiftLabel.getAppearance().setFont(_fengMarker);
		_shiftLabel.getAppearance().setTextColor(Color.WHITE);
		
		_display.addWidget(_shiftLabel);
		
		_input.setEnabled(false);
	}
	
	@Override
	public void acquireFocus() {
		// TODO Auto-generated method stub
		System.out.println("FengGUI has Focus");
		_display.setFocusedWidget(_textEditor);
		setInputEnabled(true);
	}

	@Override
	public float getPctDone() {
		return 1;
	}

	public Map getMap() {
		return _map;
	}
	
	/**
	 * must be there now.  You can fill it out later.
	 */
	public void showHelpScreen() {
		Window w = createHelpScreen();	
		
		w.layout();
		_display.addWidget(w);
		_display.layout();
	}
	
	protected Window createHelpScreen() {
		int halfWidth = DisplaySystem.getDisplaySystem().getWidth() / 2;
		int halfHeight = DisplaySystem.getDisplaySystem().getHeight() / 2;

		Window w = makeWindow("Help", halfWidth - 450, halfHeight - 150, 710, 430);
		
		Label l = makePixmapLabel("signature.png", 30, 30, 152, 53);
		w.addWidget(l);
		
		String txt = 
			"Work with your team to move sheep into the goal of\n" +
			"your team's color, and keep the other team from getting\n" + 
			"sheep into their goal! POWER-UPS will appear to help your\n" +
			"wubble grow stronger, but only for a time. Make sure to\n" +
			"keep the sheep out of your garden, or your team's energy\n" +
			"will be drained!\n" +
			"If you have a microphone, you can talk to your friends by\n" +
			"holding down the SHIFT key. Try it!";
		
		l = makeLabel(txt, _fengComics, 20, 70, 390, 350);
		w.addWidget(l);
		
		String welcome = "SHEEP";
		l = makeLabel(welcome, _fengComics, 20, 355, 390, 30);
		w.addWidget(l);
		
		l = makePixmapLabel("keymap.png", 430, 270, 164, 101);
		w.addWidget(l);
		
		l = makePixmapLabel("home-off.png", 430, 230, 45, 45);
		w.addWidget(l);
		l = makeLabel("MAIN MENU", _fengComics, 475, 235, 120, 30);
		w.addWidget(l);
		
		l = makePixmapLabel("help-off.png", 430, 190, 45, 45);
		w.addWidget(l);
		l = makeLabel("HELP SCREEN", _fengComics, 475, 195, 120, 30);
		w.addWidget(l);
		
		return w;
	}
	
	@Override
	protected void addListeners() {
		super.addListeners();
		
		_homeButton.addButtonPressedListener(new IButtonPressedListener() {
			public void buttonPressed(ButtonPressedEvent e) {
				SheepGameState.needToExit = false;
				
				Main.inst().stopState(SheepGUIState.class.getName());
				Main.inst().stopState(SheepGameState.class.getName());
				Main.inst().startState(MenuGameState.class.getName());
			}
		});
	}

	// Perhaps I should override the super instead?
	// Currently your text will be a different color
	public void handleChat(final String name, Integer team, final String text) {
		TextStyle style = _boldStyle;
		
		if (team.equals(Constants.BLUE_TEAM)) {
			style = _blueStyle;
		} else if (team.equals(Constants.RED_TEAM)) {
			style = _redStyle;
		} else {
			System.out.println("WTF?");
		}
		
		final TextStyle finalStyle = style;
		if (Main.inst().inOpenGL()) {
			_textArea.addTextLine(name + ": ", finalStyle);
			_textArea.appendText(text, _plainStyle);
		} else {
			Callable<?> callable = new Callable<Object>() {
				public Object call() throws Exception {
					_textArea.addTextLine(name + ": ", finalStyle);
					_textArea.appendText(text, _plainStyle);
					return null;
				}
			};
			GameTaskQueueManager.getManager().update(callable);
		}
	}
		
	public void shiftDown() {
		_shiftLabel.setText("Release SHIFT to Stop Talking");
	} 
	
	public void shiftUp() {
		_shiftLabel.setText("Press SHIFT to Talk");
	}
}
