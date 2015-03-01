package edu.isi.wubble.login;

import java.util.concurrent.Callable;

import org.fenggui.Button;
import org.fenggui.Label;
import org.fenggui.TextEditor;
import org.fenggui.background.PixmapBackground;
import org.fenggui.composites.Window;
import org.fenggui.event.ButtonPressedEvent;
import org.fenggui.event.IButtonPressedListener;
import org.fenggui.event.IKeyTypedListener;
import org.fenggui.event.KeyTypedEvent;
import org.fenggui.layout.StaticLayout;

import com.jme.system.DisplaySystem;
import com.jme.util.GameTaskQueueManager;

import edu.isi.wubble.Main;
import edu.isi.wubble.gamestates.GUIGameState;
import edu.isi.wubble.gamestates.WubbleGameState;
import edu.isi.wubble.jgn.ConnectionManager;
import edu.isi.wubble.jgn.message.InvokeMessage;
import edu.isi.wubble.menu.MenuGameState;

public class LoginGameState extends GUIGameState {

	private TextEditor _userNameEditor;
	private TextEditor _passwordEditor;
	
	private Button _loginButton;
	private Button _createButton;
	private Button _quitButton;
	
	private int _windowWidth = 250;
	private int _windowHeight = 150;
	
	public LoginGameState() {
		super();

		Callable<Object> callable = new Callable<Object>() {
			public Object call() throws Exception {
				initGUI();
				addListeners();
				return null;
			}
		};
		GameTaskQueueManager.getManager().update(callable);
	}
	
	protected void initGUI() {
		_display.setLayoutManager(new StaticLayout());

		Label l = makePixmapLabel("Login.png", 0, 0, 1024, 768);
		_display.addWidget(l);
		
		int cWidth = DisplaySystem.getDisplaySystem().getWidth() / 2;
		int cHeight = DisplaySystem.getDisplaySystem().getHeight() / 2;
		
		_userNameEditor = makeTextEditor(cWidth+150, cHeight-40, 200, 20);
		_userNameEditor.setTraversable(true);
		_display.addWidget(_userNameEditor);
		
		_passwordEditor = makeTextEditor(cWidth+150, cHeight-70, 200, 20);
		_passwordEditor.setPasswordField(true);
		_passwordEditor.setTraversable(true);
		_display.addWidget(_passwordEditor);

		_loginButton = makeButton("Login", cWidth+185, cHeight-110, 40, 30);
		_display.addWidget(_loginButton);

		_createButton = makeButton("Create", cWidth+225, cHeight-110, 40, 30);
		_display.addWidget(_createButton);
		
		_quitButton = makeButton("Quit", cWidth+265, cHeight-110, 40, 30);
		_display.addWidget(_quitButton);
		
		_display.layout();
		_display.setFocusedWidget(_userNameEditor);
	}
	
	protected void addListeners() {
		_quitButton.addButtonPressedListener(new IButtonPressedListener() {
			public void buttonPressed(ButtonPressedEvent e) {
				Main.inst().finish();
			}
		});
		
		_loginButton.addButtonPressedListener(new IButtonPressedListener() {
			public void buttonPressed(ButtonPressedEvent e) {
				validate(_userNameEditor.getText(), _passwordEditor.getText());
			}
		});
		
		_createButton.addButtonPressedListener(new IButtonPressedListener() {
			public void buttonPressed(ButtonPressedEvent e) {
				create(_userNameEditor.getText(), _passwordEditor.getText());
			}
		});
		
		_passwordEditor.addKeyTypedListener(new IKeyTypedListener() { 
			public void keyTyped(KeyTypedEvent evt) {
				if (evt.getKey() == 10 || evt.getKey() == 13) {
					_display.setFocusedWidget(_loginButton);
					validate(_userNameEditor.getText(), _passwordEditor.getText());
				}
			}
		});
	}
	
	protected void validate(String user, String pass) {
		int cWidth = DisplaySystem.getDisplaySystem().getWidth() / 2;
		int cHeight = DisplaySystem.getDisplaySystem().getHeight() / 2;
		
		LoginClient lc = new LoginClient(user, pass);
		if (lc.authenticate()) {
			Main.inst().setName(user);
			Main.inst().setPassword(pass);
			
			Main.inst().stopState(LoginGameState.class.getName());
			Main.inst().startState(MenuGameState.class.getName());
		} else {
			Window w = makeWindow("Invalid Name and/or Password", cWidth - _windowWidth / 2, cHeight, _windowWidth, _windowHeight);
			
			String txt = "The user name/password\n" + 
						"combination you entered is \n" +
						"invalid. Please try again.";

			Label l = makeLabel(txt, _fengArialBold, 20, 10, 200, 100);
			w.addWidget(l);
			
			_display.addWidget(w);
			
			_userNameEditor.setText("");
			_passwordEditor.setText("");
		}
	}
	
	protected void create(String user, String pass) {
		int cWidth = DisplaySystem.getDisplaySystem().getWidth() / 2;
		int cHeight = DisplaySystem.getDisplaySystem().getHeight() / 2;
		
		LoginClient lc = new LoginClient(user, pass);
		String result = lc.create();
		if (result.equals("SUCCESS")) {
			Main.inst().setName(user);
			Main.inst().setPassword(pass);
			
			Main.inst().stopState(LoginGameState.class.getName());
			Main.inst().startState(MenuGameState.class.getName());
		} else if (result.equals("INVALID")){
			Window w = makeWindow("Invalid Name and/or Password", cWidth - _windowWidth / 2, cHeight, _windowWidth, _windowHeight);
			
			String txt = "Something you entered contained\n" + 
						"a space. Please try again with no\n" +
						"spaces.";

			Label l = makeLabel(txt, _fengArialBold, 20, 10, 200, 100);
			w.addWidget(l);
			
			_display.addWidget(w);
			
			_userNameEditor.setText("");
			_passwordEditor.setText("");
		} else if (result.equals("EXISTS")) {
			Window w = makeWindow("User Name Taken", cWidth - _windowWidth / 2, cHeight, _windowWidth, _windowHeight);
			
			String txt = "The user name you requested\n" + 
						"is taken. Please choose another\n" +
						"and try again.";
			
			Label l = makeLabel(txt, _fengArialBold, 20, 10, 200, 100);
			w.addWidget(l);
			
			_display.addWidget(w);
			
			_userNameEditor.setText("");
			_passwordEditor.setText("");
		} else {
			Window w = makeWindow("Account Creation Error", cWidth - _windowWidth / 2, cHeight, _windowWidth, _windowHeight);
			
			String txt = "Sorry, there was an error in\n" + 
						"creating your account, please\n" +
						"try again later.";

			Label l = makeLabel(txt, _fengArialBold, 20, 10, 200, 100);
			w.addWidget(l);
			
			_display.addWidget(w);
			
			_userNameEditor.setText("");
			_passwordEditor.setText("");
		}
	}
	
	public void setActive(boolean status) {
		super.setActive(status);
		if (status)
			acquireFocus();
	}

	/**
	 * someone passed the torch to us, we need to set our focused
	 * widget.
	 */
	public void acquireFocus() {
		_display.setFocusedWidget(_userNameEditor);
	}

	/**
	 * 
	 */
	public float getPctDone() {
		return 1.0f;
	}
}
