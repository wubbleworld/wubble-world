package edu.isi.wubble.rpg;

import static edu.isi.wubble.jgn.message.InvokeMessage.createMsg;

import java.util.concurrent.Callable;

import org.fenggui.Button;
import org.fenggui.Label;
import org.fenggui.background.PlainBackground;
import org.fenggui.composites.MessageWindow;
import org.fenggui.composites.Window;
import org.fenggui.event.ButtonPressedEvent;
import org.fenggui.event.IButtonPressedListener;
import org.fenggui.layout.StaticLayout;
import org.fenggui.util.Color;
import org.fenggui.util.Point;

import com.captiveimagination.jgn.event.MessageListener;
import com.captiveimagination.jgn.message.Message;
import com.jme.system.DisplaySystem;
import com.jme.util.GameTaskQueueManager;

import edu.isi.wubble.Main;
import edu.isi.wubble.gamestates.GUIGameState;
import edu.isi.wubble.jgn.ConnectionManager;
import edu.isi.wubble.jgn.message.GameStatusMessage;
import edu.isi.wubble.jgn.message.InvokeMessage;

public class RPGLoginState extends GUIGameState implements MessageListener {

	private Button _quitButton;
	private Button _refreshButton;
	
	protected boolean _playing;
	
	public RPGLoginState() {
		super();
		
		initGUI();
		addListeners();
		
		_playing = false;
	}
	
	protected void initGUI() {
		_display.setLayoutManager(new StaticLayout());

		int halfWidth = DisplaySystem.getDisplaySystem().getWidth() / 2;

		_quitButton = makeButton("Quit", halfWidth-65, 10, 60, 30);
		_display.addWidget(_quitButton);
		
		_refreshButton = makeButton("Refresh", halfWidth+5, 10, 60, 30);
		_display.addWidget(_refreshButton);
		
		_display.layout();
	}
	
	protected void addListeners() {
		_quitButton.addButtonPressedListener(new IButtonPressedListener() {
			public void buttonPressed(ButtonPressedEvent e) {
				Main.inst().finish();
			}
		});
		
		_refreshButton.addButtonPressedListener(new IButtonPressedListener() {
			public void buttonPressed(ButtonPressedEvent e) {
				InvokeMessage msg = createMsg("refresh", new Object[] { ConnectionManager.inst().getClientId() });
				msg.sendToServer();
			}
		});
	}
	
	/**
	 * someone passed the torch to us, we need to set our focused
	 * widget.
	 */
	public void acquireFocus() {
	}

	/**
	 * 
	 */
	public float getPctDone() {
		return 1.0f;
	}
	
	protected Button setupButton(String name, String alt, int index, int role, int yPos) {
		int halfWidth = DisplaySystem.getDisplaySystem().getWidth() / 2;
		Button b = null;
		if (name != null) {
			b = makeButton("Playing: " + name, halfWidth-50, yPos, 100, 25);
			b.setEnabled(false);
		} else {
			b = makeButton("Play as " + alt, halfWidth-50, yPos, 100, 25);
			b.setEnabled(true);
			addButtonListener(b, index, role);
		}
		return b;
	}
	
	protected void addButtonListener(Button b, final int index, final int role) {
		b.addButtonPressedListener(new IButtonPressedListener() {
			public void buttonPressed(ButtonPressedEvent e) {
				InvokeMessage msg = createMsg("joinGame", new Object[] {
					ConnectionManager.inst().getClientId(),
					Main.inst().getName(), Main.inst().getPassword(), 
					index, role 
				});
				msg.sendToServer();
				RPGManager.setRole(role);
			}
		});
	}
	
	protected void addGameInfo(final int index, String sName, String pName, int done) {
		int halfWidth = DisplaySystem.getDisplaySystem().getWidth() / 2;
		int height = DisplaySystem.getDisplaySystem().getHeight() - 25;  // 25 is an offset
		
		String text = "Percent Done: " + done + "%";
		Label l = makeLabel(text, _fengArialBold, halfWidth-50, height-(index*100), 200, 25);
		
		Button shooter = setupButton(sName, "Shooter", index, 0, height-(index*100+30));
		Button picker = setupButton(pName, "Picker", index, 1, height-(index*100+60));
		
		_display.addWidget(l);
		_display.addWidget(shooter);
		_display.addWidget(picker);
	}
		
	protected void processMessage(GameStatusMessage msg) {
		String[] shooterArray = msg.getShooterArray();
		String[] pickerArray = msg.getPickerArray();
		int[] doneArray = msg.getAmountDoneArray();
		
		_display.removeAllWidgets();
		_display.addWidget(_quitButton);
		_display.addWidget(_refreshButton);
		
		for (int i = 0; i < shooterArray.length; ++i) {
			System.out.println("information: " + shooterArray[i] + " " + pickerArray[i] + " " + doneArray[i]);
			addGameInfo(i, shooterArray[i], pickerArray[i], doneArray[i]);
		}
		_display.layout();
	}
	
	public void cleanup() {
		if (!_playing) {
			System.out.println("Playing = false");
			ConnectionManager.inst().disconnect();
		}
	}
	
	public void success() {
		_playing = true;
		RPGManager.startRpgGame();
	}
	
	public void failure(String reason) {
		int halfWidth = DisplaySystem.getDisplaySystem().getWidth() / 2;
		int halfHeight = DisplaySystem.getDisplaySystem().getHeight() / 2;
		
		MessageWindow mw = new MessageWindow(true, false, false);
		mw.getAppearance().removeAll();
		mw.getAppearance().add(new PlainBackground(new Color(0,0,0, 0.3f)));
		mw.getLabel().setText("Sorry! " + reason);
		mw.setPosition(new Point(halfWidth-100, halfHeight-100));
		mw.setSize(200,200);
	}
	
	//--------------------------------------
	// jgn callbacks
	//--------------------------------------
	
	public void messageCertified(Message message) { }
	public void messageFailed(Message message) { }
	public void messageSent(Message message) { }

	public void messageReceived(final Message message) {
		if (message instanceof GameStatusMessage) {
			Callable<?> callable = new Callable<Object>() {
				public Object call() throws Exception {
					processMessage((GameStatusMessage) message);
					return null;
				}
			};
			GameTaskQueueManager.getManager().update(callable);
		} else if (message instanceof InvokeMessage) {
			((InvokeMessage) message).callMethod(this);
		}
	}
	
}
