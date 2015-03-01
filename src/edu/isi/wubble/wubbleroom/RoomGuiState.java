package edu.isi.wubble.wubbleroom;

import java.util.TreeMap;

import org.fenggui.Button;
import org.fenggui.Label;
import org.fenggui.composites.Window;
import org.fenggui.event.ButtonPressedEvent;
import org.fenggui.event.IButtonPressedListener;
import org.fenggui.layout.StaticLayout;
import org.fenggui.render.Pixmap;
import org.fenggui.util.Color;
import org.fenggui.util.Point;

import com.jme.system.DisplaySystem;

import edu.isi.wubble.Main;
import edu.isi.wubble.gamestates.DefaultGuiGameState;

public class RoomGuiState extends DefaultGuiGameState {

	private static TreeMap<String,String> _nameMap;
	
	static {
		_nameMap = new TreeMap<String,String>();
		_nameMap.put(BeginnerRoomState.class.getName(), "room-beginner.png");
		_nameMap.put(IntermediateRoomState.class.getName(), "room-middle.png");
		_nameMap.put(AdvancedRoomState.class.getName(), "room-advanced5.png");
	}

	protected Window _helpScreen;
	protected Button _pokeButton;
	
	public RoomGuiState() {
		super();
		
		initGui();
		
		makeHelpScreen();
		setupHelpScreen();
	}

	protected void initGui() {
		_display.setLayoutManager(new StaticLayout());

		initPlayersWidget();
		
		_display.layout();
	}
	
	public void setRoom(String roomName) {
		int width = _display.getWidth();
		int height = _display.getHeight();

		Window w = makePlainWindow(width-278, height-159, 270, 150);
		String texFile = _nameMap.get(roomName);
		Label l = makePixmapLabel(makePixmap(texFile), 10, 10, 243, 124);
		w.addWidget(l);
		w.layout();
		
		_display.addWidget(w);
		_display.layout();
	}
	
	public void setRoom(String roomName, int scenario) {
		
	}
	
	protected void initButtonArea() {
		_display.addWidget(makePixmapLabel("ButtonBarMiddle.png", 459, 5, 139, 165));
			
		_homeButton = makePixmapButton("home-off.png", "home-over.png", 459, 10, 48, 48);
		_display.addWidget(_homeButton);
			
		_helpButton = makePixmapButton("help-off.png", "help-over.png", 506, 10, 48, 48);
		_display.addWidget(_helpButton);
			
		_pokeButton = makePixmapButton("poke-off.png", "poke-over.png", 553, 10, 48, 48);
		_display.addWidget(_pokeButton);
			
		_display.addWidget(makePixmapLabel("ButtonBarRight.png", 598, 5, 36, 165));
	}
	
	protected void addListeners() {
		super.addListeners();
		_pokeButton.addButtonPressedListener(new IButtonPressedListener() {
			public void buttonPressed(ButtonPressedEvent e) {
				RoomManager.getChat().chatMsg("You", "Wubble can you hear me?");
				SocketClient.inst().sendMessage("awake");
			}
		});

		_homeButton.addButtonPressedListener(new IButtonPressedListener() {
			public void buttonPressed(ButtonPressedEvent e) {
				RoomManager.stopGame();
			}
		});
	}

	protected void initPlayersWidget() {
		int height = _display.getHeight();
		Pixmap pixmap = makePixmap("widget.png");

		_display.addWidget(makePixmapLabel(pixmap, 10, height-74, 214, 77));
		_display.addWidget(makeNameLabel(Main.inst().getName(), 100, height-42));
	}
	
	protected Label makeNameLabel(String name, int x, int y) {
		Label nameLabel = new Label(name);
		nameLabel.setPosition(new Point(x, y));
		nameLabel.setSize(100, 20);
		nameLabel.getAppearance().setFont(_fengMarker);
		nameLabel.getAppearance().setTextColor(new Color(1.0f,1.0f,1.0f,1.0f));
		return nameLabel;
	}
	
	public float getPctDone() {
		return 0;
	}
	
	protected void makeHelpScreen() {
		int halfWidth = DisplaySystem.getDisplaySystem().getWidth() / 2;
		int halfHeight = DisplaySystem.getDisplaySystem().getHeight() / 2;

		Window w = makeWindow("Help", halfWidth-350, halfHeight-210, 710, 430);
		
		String welcome = "WELCOME TO YOUR WUBBLE'S ROOM";
		w.addWidget(makeLabel(welcome, _fengComics, 20, 355, 390, 30));
		
		String txt = 
			"Your wubble is like a little kid who doesn't understand\n" +
			"language very well, so you have to say things to it the way\n" +
			"you would talk to your baby sister or brother, and sometimes\n" +
			"you have to say things in different ways. As your wubble\n" +
			"grows up, it gets better at understanding what you say to it.\n\n" +
			"Each time you enter this room you are presented with a task to\n" +
			"complete with the help of your wubble.  After completing the\n" +
			"task, you can try more challenging rooms.\n";
		w.addWidget(makeLabel(txt, _fengComicsSmall, 20, 115, 390, 350));
		w.addWidget(makePixmapLabel("signature.png", 119, 165, 152, 53));
		
		txt = 
			"HINT: Your wubble has a short attention span. Make\n" +
			"sure that your wubble is playing with the right objects\n" +
			"by focusing your wubble's attention where you want it\n" +
			"You can focus the wubble's attention when you tell the\n" +
			"wubble to CHOOSE\n\n" +
			"EXAMPLE:  choose a red sphere\n\n" +
			"HINT: The slash key (/)  is used to quickly switch to\n" +
			"the chat box";
		w.addWidget(makeLabel(txt, _fengComicsSmall, 20, 10, 390, 150));
		
		w.addWidget(makeLabel("CONTROLS", _fengComics, 415, 355, 300, 30));
		w.addWidget(makeLabel("Left Mouse Button", _fengComics, 420, 335, 300, 20));
		
		txt = "Use the left mouse button to select\nobjects for your wubble.";
		w.addWidget(makeLabel(txt, _fengComics, 425, 295, 300, 40));
		
		w.addWidget(makeLabel("Right Mouse Button", _fengComics, 420, 275, 300, 20));
		
		txt = "Hold down and move the mouse to change \n" +
			"the camera view. Moving the mouse up\n" +
			"and down changes the height of the\n" +
			"camera. Moving the mouse left and \n" +
			"right changes the location of the \n" +
			"camera. When you release the button\n" +
			"the camera will return to its starting\n" + 
			"position.";
		w.addWidget(makeLabel(txt, _fengComics, 425, 142, 300, 140));
		
		w.addWidget(makePixmapLabel("home-off.png", 430, 100, 48, 48));
		w.addWidget(makeLabel("MAIN MENU", _fengComics, 475, 100, 120, 30));
		
		w.addWidget(makePixmapLabel("help-off.png", 430, 55, 48, 48));
		w.addWidget(makeLabel("HELP SCREEN", _fengComics, 475, 55, 120, 30));
		
		w.addWidget(makePixmapLabel("poke-off.png", 430, 10, 48, 48));
		w.addWidget(makeLabel("POKE WUBBLE", _fengComics, 475, 10, 120, 30));

		w.layout();
		_helpScreen = w;
	}

	/**
	 * fill up this help screen with relevant information that is
	 * useful for each room.
	 */
	public void showHelpScreen() {
		if (!_helpScreen.isInWidgetTree()) {
			_display.addWidget(_helpScreen);
			_display.layout();
		}
	}
	
}
