package edu.isi.wubble.gamestates;

import java.util.concurrent.Callable;

import org.fenggui.Button;
import org.fenggui.Label;
import org.fenggui.TextEditor;
import org.fenggui.background.PixmapBackground;
import org.fenggui.background.PlainBackground;
import org.fenggui.composites.Window;
import org.fenggui.event.ButtonPressedEvent;
import org.fenggui.event.IButtonPressedListener;
import org.fenggui.event.IKeyTypedListener;
import org.fenggui.event.KeyTypedEvent;
import org.fenggui.layout.StaticLayout;
import org.fenggui.render.Pixmap;
import org.fenggui.text.TextStyle;
import org.fenggui.util.Color;
import org.fenggui.util.Point;
import org.fenggui.util.Spacing;

import com.jme.system.DisplaySystem;
import com.jme.util.GameTaskQueueManager;

import edu.isi.wubble.Main;
import edu.isi.wubble.ScrollingTextView;
import edu.isi.wubble.jgn.ConnectionManager;
import edu.isi.wubble.jgn.message.InvokeMessage;
import edu.isi.wubble.wubbleroom.SocketClient;

public abstract class DefaultGuiGameState extends GUIGameState {

	protected TextEditor        _textEditor;
	protected ScrollingTextView _textArea;
	
	protected TextStyle _boldStyle;
	protected TextStyle _plainStyle;
	
	protected Button _helpButton;
	protected Button _homeButton;
	
	protected IKeyTypedListener _sendListener;
	
	public DefaultGuiGameState() {
		super();
		
		initGUI();
		addListeners();
	}
	
	protected void initGUI() {
		_display.setLayoutManager(new StaticLayout());
		_plainStyle = new TextStyle(_fengArial, Color.WHITE);
		_boldStyle = new TextStyle(_fengArialBold, Color.WHITE);
		
		initTextArea();
		initButtonArea();
		
		_display.layout();
		chatMsg("Welcome to Wubble World", "");
	}
	
	protected void initTextArea() {
		Label l = makePixmapLabel("ChatBoxSelf.png", 5, 5, 454, 165);
		_display.addWidget(l);
		
		_textEditor = new TextEditor(false);
		_textEditor.setText("");
		_textEditor.setPosition(new Point(35, 17));
		_textEditor.setSize(370, 20);
		
		_textEditor.getAppearance().removeAll();
		_textEditor.getAppearance().add(new PlainBackground(new Color(0.5f, 0.5f, 0.5f, 0.3f)));
		_textEditor.getAppearance().setFont(_fengArial);
		_textEditor.getAppearance().setTextColor(Color.WHITE);
		_textEditor.getAppearance().setPadding(new Spacing(2, 2));

		_display.addWidget(_textEditor);
		
		_textArea = new ScrollingTextView();
		_textArea.setPosition(new Point(35,37));
		_textArea.setSize(370,113);
		
		_textArea.getAppearance().removeAll();
		_textArea.getTextView().getAppearance().removeAll();
		_textArea.getTextView().getAppearance().setPadding(new Spacing(2, 2));
		_textArea.getVerticalScrollBar().getIncreaseButton().getAppearance().removeAll();
		_textArea.layout();
		_textArea.scrollVertical(0);
		
		_display.addWidget(_textArea);
	}
	
	protected void initButtonArea() {
		Label l = makePixmapLabel("ButtonBarMiddle.png", 459, 5, 92, 165);
		_display.addWidget(l);
		
		_homeButton = makePixmapButton("home-off.png", "home-over.png", 459, 10, 48, 48);
		_display.addWidget(_homeButton);
		
		_helpButton = makePixmapButton("help-off.png", "help-over.png", 506, 10, 48, 48);
		_display.addWidget(_helpButton);
		
		l = makePixmapLabel("ButtonBarRight.png", 551, 5, 36, 165);
		_display.addWidget(l);
	}
	
	protected void addListeners() {
		widgetAddFocus(_textEditor, getClass().getName());
		
		_sendListener = new IKeyTypedListener() {
			public void keyTyped(KeyTypedEvent evt) {
				if (evt.getKey() == 10 || evt.getKey() == 13) {
					short id = ConnectionManager.inst().getClientId();
					InvokeMessage message = new InvokeMessage();
					message.setMethodName("sendChatMessage");
					message.setArguments(new Object[] { id, _textEditor.getText() });
					ConnectionManager.inst().getClient().sendToServer(message);

					chatMsg("You", _textEditor.getText());
					_textEditor.setText("");
					
					_display.setFocusedWidget(null);
				}
			}
		};
		_textEditor.addKeyTypedListener(_sendListener);
	}
	
	/**
	 * stupid method since I need to send messages different for
	 * the lisp connection from the chat fields.
	 */
	public void addSocketListeners() {
		_textEditor.removeKeyTypedListener(_sendListener);
		_sendListener = new IKeyTypedListener() {
			public void keyTyped(KeyTypedEvent evt) {
				if (evt.getKey() == 10 || evt.getKey() == 13) {
					SocketClient.inst().sendMessage("sentence " + _textEditor.getText());

					chatMsg("You", _textEditor.getText());
					_textEditor.setText("");
					
					_display.setFocusedWidget(null);
				}
			}
		};
		_textEditor.addKeyTypedListener(_sendListener);
	}
	
	public void setupHelpScreen() {
		_helpButton.addButtonPressedListener(new IButtonPressedListener() {
			public void buttonPressed(ButtonPressedEvent e) {
				showHelpScreen();
			}
		});
	}
	
	public abstract void showHelpScreen();
	
	public void disableInput() {
		_textEditor.setEnabled(false);
	}
	
	public void enableInput() {
		_textEditor.setEnabled(true);
	}
	
	public void systemMessage(final String message) {
		if (Main.inst().inOpenGL()) {
			_textArea.addTextLine(message, _boldStyle);
		} else {
			Callable<?> callable = new Callable<Object>() {
				public Object call() throws Exception {
					_textArea.addTextLine(message, _boldStyle);
					return null;
				}
			};
			GameTaskQueueManager.getManager().update(callable);
		}
	}
	
	public void chatMsg(final String user, final String message) {
		if (Main.inst().inOpenGL()) {
			_textArea.addTextLine(user + ": ", _boldStyle);
			_textArea.appendText(message, _plainStyle);
		} else {
			Callable<?> callable = new Callable<Object>() {
				public Object call() throws Exception {
					_textArea.addTextLine(user + ": ", _boldStyle);
					_textArea.appendText(message, _plainStyle);
					return null;
				}
			};
			GameTaskQueueManager.getManager().update(callable);
		}
	}
	
	public void speak(final String message) {
		if (Main.inst().inOpenGL()) {
			_textArea.addTextLine("Wubble: ", _boldStyle);
			_textArea.appendText(message, _plainStyle);
		} else {
			Callable<?> callable = new Callable<Object>() {
				public Object call() throws Exception {
					_textArea.addTextLine("Wubble: ", _boldStyle);
					_textArea.appendText(message, _plainStyle);
					return null;
				}
			};
			GameTaskQueueManager.getManager().update(callable);
		}
	}
	
	/**
	 * someone passed the torch to us.  Time to enable our
	 * input and set our active object.
	 */
	public void acquireFocus() {
		_display.setFocusedWidget(_textEditor);
	}
	
	@Override
	public float getPctDone() {
		// TODO Auto-generated method stub
		return 0;
	}

}
