package edu.isi.wubble.rpg;

import static com.jme.util.resource.ResourceLocatorTool.TYPE_TEXTURE;

import java.awt.image.BufferedImage;
import java.net.URL;

import javax.imageio.ImageIO;

import org.fenggui.Button;
import org.fenggui.Label;
import org.fenggui.background.PixmapBackground;
import org.fenggui.composites.Window;
import org.fenggui.event.ButtonPressedEvent;
import org.fenggui.event.IButtonPressedListener;
import org.fenggui.layout.StaticLayout;
import org.fenggui.render.Binding;
import org.fenggui.render.Pixmap;
import org.fenggui.util.Color;
import org.fenggui.util.Point;

import com.jme.util.resource.ResourceLocatorTool;

import edu.isi.wubble.Main;
import edu.isi.wubble.gamestates.GUIGameState;

public class RPGLosingState extends GUIGameState {

	protected Button _quitButton;
	protected Button _tryAgainButton;
	
	public RPGLosingState() {
		super();
		
		initGui();
	}
	
	protected void initGui() {
		_display.setLayoutManager(new StaticLayout());
		int width = _display.getWidth();
		int height = _display.getHeight();
		
		Window w = new Window(false,false,false);
		w.removeAllWidgets();
		w.getAppearance().removeAll();
		w.setSize(width,height);

		try {
			URL url = ResourceLocatorTool.locateResource(TYPE_TEXTURE, "loseRPG.png");
			BufferedImage bi = ImageIO.read(url);
			Pixmap pixmap = new Pixmap(Binding.getInstance().getTexture(bi));
			w.getAppearance().add(new PixmapBackground(pixmap));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		Label loserLabel = new Label("Sorry! The enemies have driven you away!");
		loserLabel.setPosition(new Point(10,height-100));
		loserLabel.setSize(width,40);
		loserLabel.getAppearance().setTextColor(Color.WHITE);
		loserLabel.getAppearance().setFont(_fengMarker);
		w.addWidget(loserLabel);
		
		_quitButton = new Button("quit");
		_quitButton.setPosition(new Point(30,height-120));
		_quitButton.setSize(50,20);
		w.addWidget(_quitButton);
		
		w.layout();
		
		_display.addWidget(w);
		_display.layout();
	}
	
	protected void addListeners() {
		_quitButton.addButtonPressedListener(new IButtonPressedListener() {
			public void buttonPressed(ButtonPressedEvent e) {
				RPGManager.startLogin();
				
				Main.inst().stopState(RPGLosingState.class.getName());
			}
		});
	}
	
	public void acquireFocus() {
		
	}

	public float getPctDone() {
		return 0;
	}
}
