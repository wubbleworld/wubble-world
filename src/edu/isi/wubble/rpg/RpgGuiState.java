package edu.isi.wubble.rpg;

import static edu.isi.wubble.jgn.message.InvokeMessage.createMsg;
import static com.jme.util.resource.ResourceLocatorTool.TYPE_TEXTURE;

import java.awt.image.BufferedImage;
import java.net.URL;

import javax.imageio.ImageIO;

import org.fenggui.Button;
import org.fenggui.Label;
import org.fenggui.TextEditor;
import org.fenggui.background.PixmapBackground;
import org.fenggui.background.PlainBackground;
import org.fenggui.composites.Window;
import org.fenggui.event.ButtonPressedEvent;
import org.fenggui.event.IButtonPressedListener;
import org.fenggui.layout.StaticLayout;
import org.fenggui.render.Binding;
import org.fenggui.render.Pixmap;
import org.fenggui.util.Color;
import org.fenggui.util.Point;
import org.fenggui.util.Spacing;

import com.jme.system.DisplaySystem;
import com.jme.util.resource.ResourceLocatorTool;

import edu.isi.wubble.gamestates.DefaultGuiGameState;
import edu.isi.wubble.jgn.ConnectionManager;
import edu.isi.wubble.jgn.message.InvokeMessage;
import edu.isi.wubble.jgn.rpg.RPGPhysics;

public class RpgGuiState extends DefaultGuiGameState {

	private Label _coinsLeftLabel;
	private Label _shooterHealthLabel;
	private Label _pickerHealthLabel;
	
	private Button _pickUpButton;
	private Button _putDownButton;
	private Button _standUpButton;
	
	private TextEditor _jumbledTF;
	private TextEditor _englishTF;
	
	public RpgGuiState() {
		super();	
	}

	protected void initGUI() {
		super.initGUI();
		
		initCoinsWidget();
		initPlayersWidget();
		
		initGuessWidget();
		
		setupHelpScreen();
		showHelpScreen();
		
		_display.layout();
	}
	
	protected void initButtonArea() {
		if (RPGManager.getRole() == 0) {
			super.initButtonArea();
		} else {
			Label l = makePixmapLabel("ButtonBarMiddle.png", 459, 5, 233, 165);
			_display.addWidget(l);
			
			_homeButton = makePixmapButton("home-off.png", "home-over.png", 459, 10, 48, 48);
			_display.addWidget(_homeButton);
			
			_helpButton = makePixmapButton("help-off.png", "help-over.png", 506, 10, 48, 48);
			_display.addWidget(_helpButton);
			
			_pickUpButton = makePixmapButton("up-off.png", "up-over.png", 553, 10, 48, 48);
			_display.addWidget(_pickUpButton);
			
			_putDownButton = makePixmapButton("down-off.png", "down-over.png", 600, 10, 48, 48);
			_display.addWidget(_putDownButton);
			
			_standUpButton = makePixmapButton("orient-off.png", "orient-over.png", 647, 10, 48, 48);
			_display.addWidget(_standUpButton);
			
			l = makePixmapLabel("ButtonBarRight.png", 692, 5, 36, 165);
			_display.addWidget(l);
		}
	}
	
	protected void initCoinsWidget() {
		int width = _display.getWidth();
		int height = _display.getHeight();
		
		_coinsLeftLabel = new Label("0/5");
		_coinsLeftLabel.setPosition(new Point(width-64, height-104));
		_coinsLeftLabel.setSize(100,20);
		_coinsLeftLabel.getAppearance().setTextColor(new Color(1.0f,1.0f,1.0f,1.0f));
		_coinsLeftLabel.getAppearance().setFont(_fengMarker);
		_display.addWidget(_coinsLeftLabel);

		Label coinLabel = new Label("");
		coinLabel.setPosition(new Point(width-74,height-74));
		coinLabel.setSize(64,64);
		_display.addWidget(coinLabel);

		try {
			URL url = ResourceLocatorTool.locateResource(TYPE_TEXTURE, "hud-coin.png");
			BufferedImage bi = ImageIO.read(url);
			Pixmap pixmap = new Pixmap(Binding.getInstance().getTexture(bi));
			coinLabel.getAppearance().add(new PixmapBackground(pixmap, false));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	protected void initPlayersWidget() {
		int height = _display.getHeight();

		Pixmap pixmap = makePixmap("widget.png");

		_display.addWidget(makePixmapLabel(pixmap, 10, height-74, 214, 77));
		_display.addWidget(makePixmapLabel(pixmap, 10, height-148, 214, 77));
		
		_display.addWidget(makeNameLabel("shooter", 100, height-42));
		_display.addWidget(makeNameLabel("lifter", 100, height-114));

		_shooterHealthLabel = makeHealthLabel(100, height-52);
		_pickerHealthLabel = makeHealthLabel(100, height-124);
		
		_display.addWidget(_shooterHealthLabel);
		_display.addWidget(_pickerHealthLabel);
	}
	
	protected void initGuessWidget() {
		Window w = makePlainWindow(10, 170, 210, 45);
		
		_jumbledTF = makeEditor(12,10,60,20);
		_englishTF = makeEditor(112,10,60,20);
		
		Label l = new Label("means");
		l.setPosition(new Point(72, 10));
		l.setSize(42, 20);
		
		l.getAppearance().removeAll();
		l.getAppearance().setFont(_fengArial);
		l.getAppearance().setTextColor(Color.WHITE);
		l.getAppearance().setPadding(new Spacing(2, 2));
		
		final Button b = makeButton(">", 173, 10, 20, 20);		
		b.addButtonPressedListener(new IButtonPressedListener() {
			public void buttonPressed(ButtonPressedEvent e) {
				if ("".equals(_jumbledTF.getText()) || "".equals(_englishTF.getText())) {
					RPGManager.getChat().systemMessage("You need to enter both words for translation!");
					return;
				}
				InvokeMessage msg = createMsg("tryTranslation", new Object[] { 
						ConnectionManager.inst().getClientId(),
						_jumbledTF.getText(), _englishTF.getText()
				});
				msg.sendToServer();
			}
		});
		
		w.setLayoutManager(new StaticLayout());
		w.addWidget(_jumbledTF);
		w.addWidget(l);
		w.addWidget(_englishTF);
		w.addWidget(b);
		
		widgetAddFocus(_jumbledTF, RpgGuiState.class.getName());
		widgetAddFocus(_englishTF, RpgGuiState.class.getName());

		_display.addWidget(w);
	}
	
	protected void addListeners() {
		super.addListeners();

		_homeButton.addButtonPressedListener(new IButtonPressedListener() {
			public void buttonPressed(ButtonPressedEvent e) {
				RPGManager.stopRpgGame();
			}
		});
		
		if (RPGManager.getRole() == 1) {
			_pickUpButton.addButtonPressedListener(new IButtonPressedListener() {
				public void buttonPressed(ButtonPressedEvent e) {
					short id = ConnectionManager.inst().getClientId();
					InvokeMessage message = new InvokeMessage();
					message.setMethodName("pickUp");
					message.setArguments(new Object[] { id });
					ConnectionManager.inst().getClient().sendToServer(message);
				}
			});
			
			_putDownButton.addButtonPressedListener(new IButtonPressedListener() {
				public void buttonPressed(ButtonPressedEvent e) {
					short id = ConnectionManager.inst().getClientId();
					InvokeMessage message = new InvokeMessage();
					message.setMethodName("putDown");
					message.setArguments(new Object[] { id });
					ConnectionManager.inst().getClient().sendToServer(message);
				}
			});
			
			_standUpButton.addButtonPressedListener(new IButtonPressedListener() {
				public void buttonPressed(ButtonPressedEvent e) {
					short id = ConnectionManager.inst().getClientId();
					InvokeMessage.createMsg("fixOrientation", new Object[] { id }).sendToServer();
				}
			});
		}
	}
	
	
	protected TextEditor makeEditor(int x, int y, int width, int height) {
		TextEditor tf = new TextEditor();
		tf.setText("");
		tf.setPosition(new Point(x, y));
		tf.setSize(width, height);
		
		tf.getAppearance().removeAll();
		tf.getAppearance().add(new PlainBackground(new Color(0.5f, 0.5f, 0.5f, 0.3f)));
		tf.getAppearance().setFont(_fengArial);
		tf.getAppearance().setTextColor(Color.WHITE);
		tf.getAppearance().setPadding(new Spacing(2, 2));
		
		return tf;
	}
	
	protected Label makeNameLabel(String name, int x, int y) {
		Label nameLabel = new Label(name);
		nameLabel.setPosition(new Point(x, y));
		nameLabel.setSize(100, 20);
		nameLabel.getAppearance().setFont(_fengMarker);
		nameLabel.getAppearance().setTextColor(new Color(1.0f,1.0f,1.0f,1.0f));
		return nameLabel;
	}
	
	protected Label makeHealthLabel(int x, int y) {
		Label healthLabel = new Label("");
		healthLabel.setPosition(new Point(x, y));
		healthLabel.setSize(100,10);
		healthLabel.getAppearance().add(new PlainBackground(new Color(1,0,0,0.5f)));
		return healthLabel;
	}
	
	protected Window createBaseHelp() {
		int halfWidth = DisplaySystem.getDisplaySystem().getWidth() / 2;
		int halfHeight = DisplaySystem.getDisplaySystem().getHeight() / 2;

		Window w = makeWindow("Help", halfWidth-350, halfHeight-210, 710, 430);
		
		Label l = makePixmapLabel("signature.png", 30, 30, 152, 53);
		w.addWidget(l);
		
		String txt = 
			"Help! We have been invaded by a group of small angry\n" +
			"critters that have stolen the WUBBLE WORLD coins.  I\n" +
			"need your help to get them back. We have located their\n" +
			"hideout in the caves of WUBBLE WORLD.  Your task is\n" +
			"to retrieve all of the coins and help rid us of this menace.\n\n" +
			"The critters appear to have the power to block our normal\n" +
			"means of communication.  Anytime someone speaks in their\n" +
			"presence, DIFFERENT words come out.  As a side mission\n" +
			"you need to decipher the coded words.  We've added additional\n" +
			"tools to your arsenal to help with this task.\n\n" + 
			"Using your teamwork and sleuthing skills, we will rid\n" + 
			"ourselves of these annoying creatures.\n";
		
		l = makeLabel(txt, _fengComics, 20, 70, 390, 350);
		w.addWidget(l);
		
		String welcome = "WELCOME TO YOUR WUBBLE'S ADVENTURE";
		l = makeLabel(welcome, _fengComics, 20, 355, 390, 30);
		w.addWidget(l);
		
		l = makePixmapLabel("keymap.png", 430, 270, 164, 101);
		w.addWidget(l);
		
		l = makePixmapLabel("home-off.png", 430, 230, 48, 48);
		w.addWidget(l);
		l = makeLabel("MAIN MENU", _fengComics, 475, 235, 120, 30);
		w.addWidget(l);
		
		l = makePixmapLabel("help-off.png", 430, 190, 48, 48);
		w.addWidget(l);
		l = makeLabel("HELP SCREEN", _fengComics, 475, 195, 120, 30);
		w.addWidget(l);
		
		return w;
	}
	
	protected void addPickerHelp(Window w) {
		Label l = makePixmapLabel("up-off.png", 430, 145, 48, 48);
		w.addWidget(l);
		l = makeLabel("PICK UP", _fengComics, 475, 150, 200, 30);
		w.addWidget(l);
		
		l = makePixmapLabel("down-off.png", 430, 100, 48, 48);
		w.addWidget(l);
		l = makeLabel("PUT DOWN", _fengComics, 475, 105, 200, 30);
		w.addWidget(l);
		
		l = makePixmapLabel("orient-off.png", 430, 55, 48, 48);
		w.addWidget(l);
		l = makeLabel("STAND THE OBJECT UP", _fengComics, 475, 60, 200, 30);
		w.addWidget(l);
		
		l = makeLabel("RIGHT MOUSE BUTTON -", _fengComics, 430, 30, 250, 30);
		w.addWidget(l);
		l = makeLabel("Highlights the object", _fengComics, 450, 5, 250, 30);
		w.addWidget(l);
	}
	
	protected void addShooterHelp(Window w) {
		Label l = makeLabel("LEFT MOUSE BUTTON -", _fengComics, 430, 145, 250, 30);
		w.addWidget(l);
		l = makeLabel("Fires the crossbow", _fengComics, 450, 115, 250, 30);
		w.addWidget(l);

		l = makeLabel("RIGHT MOUSE BUTTON -", _fengComics, 430, 85, 250, 30);
		w.addWidget(l);
		l = makeLabel("Highlights the object", _fengComics, 450, 55, 250, 30);
		w.addWidget(l);
	}
	
	public void showHelpScreen() {
		Window w = createBaseHelp();
		
		if (RPGManager.getRole() == 0) {
			addShooterHelp(w);
		} else {
			addPickerHelp(w);
		}
		
		w.layout();
		_display.addWidget(w);
		_display.layout();
	}
	
	public void translateResult(Boolean result) {
		_jumbledTF.setText("");
		_englishTF.setText("");

		if (result) 
			systemMessage("That is a correct translation!");
		else 
			systemMessage("Sorry that translation is not correct.");
	}
	
	public void updateHealth(Integer role, Integer health) {
		if (health.intValue() < 0) {
			health = new Integer(0);
		}
		
		if (role.intValue() == RPGPhysics.SHOOTER) {
			_shooterHealthLabel.setSize(health*5, 10);
		} else {
			_pickerHealthLabel.setSize(health*5, 10);
		}
	}
	
	public void updateCoinsLeft(Integer coinsFound) {
		_coinsLeftLabel.setText(coinsFound + "/5");
	}
	
	public float getPctDone() {
		return 0;
	}
	
}
