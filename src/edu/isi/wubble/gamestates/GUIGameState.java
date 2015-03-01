package edu.isi.wubble.gamestates;

import static com.jme.util.resource.ResourceLocatorTool.TYPE_TEXTURE;

import java.awt.image.BufferedImage;
import java.net.URL;

import javax.imageio.ImageIO;

import org.fenggui.Button;
import org.fenggui.Display;
import org.fenggui.Label;
import org.fenggui.ObservableWidget;
import org.fenggui.TextEditor;
import org.fenggui.background.GradientBackground;
import org.fenggui.background.PixmapBackground;
import org.fenggui.background.PlainBackground;
import org.fenggui.composites.Window;
import org.fenggui.event.ButtonPressedEvent;
import org.fenggui.event.FocusEvent;
import org.fenggui.event.IButtonPressedListener;
import org.fenggui.event.IFocusListener;
import org.fenggui.event.Key;
import org.fenggui.event.mouse.MouseButton;
import org.fenggui.render.Binding;
import org.fenggui.render.Font;
import org.fenggui.render.Pixmap;
import org.fenggui.render.lwjgl.LWJGLBinding;
import org.fenggui.switches.SetPixmapSwitch;
import org.fenggui.util.Color;
import org.fenggui.util.Point;

import com.jme.bounding.BoundingVolume;
import com.jme.input.InputHandler;
import com.jme.input.MouseInput;
import com.jme.input.MouseInputListener;
import com.jme.input.action.InputActionEvent;
import com.jme.input.action.KeyInputAction;
import com.jme.intersection.CollisionResults;
import com.jme.intersection.PickResults;
import com.jme.math.Ray;
import com.jme.renderer.Renderer;
import com.jme.scene.Spatial;
import com.jme.scene.shape.Quad;
import com.jme.system.DisplaySystem;
import com.jme.util.resource.ResourceLocatorTool;

import edu.isi.wubble.FengKeyMap;
import edu.isi.wubble.Main;

public abstract class GUIGameState extends WubbleGameState {

	protected FengGUISpatial _fengSpatial;
	protected Display        _display;
	protected InputHandler   _input;
	
	protected Pixmap[]       _windowPixmaps;
	protected Pixmap[]       _titlePixmaps;
	protected Pixmap[]       _plainWindowPixmaps;

	protected static boolean _fontsInitialized;
	public static Font _fengArial;
	public static Font _fengArialBold;
	public static Font _fengMarker;
	public static Font _fengComics;
	public static Font _fengComicsSmall;
	
	protected static Color BLUE = new Color(180,181,229);
	protected static Color TEXT_GREY = new Color(80,80,80);
	protected static Color GRADIENT_GREY = new Color(127,127,127);
		
	private MouseListener _mouseListener;
	
	public GUIGameState() {
		super();
		
		_display = new Display(new LWJGLBinding());
		_display.setDepthTestEnabled(true);
		
		// Added for WebStart, but seems to be OK generally
		Binding.getInstance().setUseClassLoader(true);
		
		if (!_fontsInitialized) {
			_fontsInitialized = true;
			
			try {
				_fengArial       = makeFont("Arial");
				_fengArialBold   = makeFont("ArialBold");
				_fengMarker      = makeFont("KomikaDisplay");
				_fengComics      = makeFont("Comics");
				_fengComicsSmall = makeFont("ComicsSmall");
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
		
		_fengSpatial = new FengGUISpatial();
		_fengSpatial.setRenderQueueMode(Renderer.QUEUE_ORTHO);

		MouseInput.get().setCursorVisible(true);

		initInput();
	}
	
	protected Font makeFont(String name) throws Exception {
		return new Font("media/data/" + name + ".png", "media/data/" + name + ".xml");
	}
	
	private void initInput() {
		_input = new InputHandler();
		KeyInputAction keyAction = new KeyInputAction() {
			public void performAction(InputActionEvent evt) {
				char character = evt.getTriggerCharacter();
				Key key = FengKeyMap.inst().getMapping();
				if(evt.getTriggerPressed()) {
					_display.fireKeyPressedEvent(character, key);
					_display.fireKeyTypedEvent(character);
				}
				else
					_display.fireKeyReleasedEvent(character, key);				
			}
		};
		_input.addAction(keyAction, InputHandler.DEVICE_KEYBOARD, InputHandler.BUTTON_ALL, InputHandler.AXIS_NONE, false);
		
		
		_mouseListener = new MouseListener(getClass().getSimpleName());
		MouseInput.get().addListener(_mouseListener);
	}
	
	protected Pixmap makePixmap(String fileName) {
		Pixmap pixmap = null;
		try {
			URL url = ResourceLocatorTool.locateResource(TYPE_TEXTURE, fileName);
			BufferedImage bi = ImageIO.read(url);
			pixmap = new Pixmap(Binding.getInstance().getTexture(bi));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return pixmap;
	}
	
	protected Button makePixmapButton(String low, String high, int x, int y, int width, int height) {
		Button b = makeButton("", x, y, width, height);
		Pixmap lowlight = makePixmap(low);
		Pixmap highlight = makePixmap(high);

		b.getAppearance().removeAll();
		b.getAppearance().add(new SetPixmapSwitch(Button.LABEL_DEFAULT, lowlight));
		b.getAppearance().add(new SetPixmapSwitch(Button.LABEL_MOUSEHOVER, highlight));
		b.getAppearance().add(new SetPixmapSwitch(Button.LABEL_FOCUSED, highlight));
		
		b.getAppearance().setEnabled(Button.LABEL_DEFAULT, true);
		
		return b;
	}

	protected Button makeButton(String name, int x, int y, int width, int height) {
		Button b = new Button(name);
		b.setPosition(new Point(x, y));
		b.setSize(width, height);
		b.getAppearance().setFont(_fengArialBold);
		b.getAppearance().setTextColor(TEXT_GREY);
		b.getAppearance().add(Button.LABEL_DEFAULT, new GradientBackground(Color.WHITE, GRADIENT_GREY));
		b.getAppearance().add(Button.LABEL_MOUSEHOVER, new GradientBackground(Color.WHITE, BLUE));
		b.getAppearance().setEnabled(Button.LABEL_MOUSEHOVER, false);
		
		return b;
	}
	
	protected Label makeLabel(String text, Font f, int x, int y, int width, int height) {
		Label l = new Label(text);
		l.setPosition(new Point(x,y));
		l.setSize(width,height);
		l.getAppearance().setTextColor(Color.WHITE);
		l.getAppearance().setFont(f);
		
		return l;
	}
	
	protected Label makePixmapLabel(Pixmap p, int x, int y, int width, int height) {
		Label l = new Label("");
		l.setPosition(new Point(x, y));
		l.setSize(width, height);
		l.getAppearance().removeAll();
		l.getAppearance().add(new PixmapBackground(p, true));
		
		return l;
	}
	
	protected Label makePixmapLabel(String img, int x, int y, int width, int height) {
		Label l = new Label("");
		l.setPosition(new Point(x, y));
		l.setSize(width, height);
		l.getAppearance().removeAll();
		l.getAppearance().add(new PixmapBackground(makePixmap(img), true));
		
		return l;
	}
	
	protected TextEditor makeTextEditor(int x, int y, int width, int height) {
		TextEditor te = new TextEditor(false);
		te.setText("");
		te.setPosition(new Point(x, y));
		te.setSize(width, height);
		te.getAppearance().setFont(_fengArial);
		return te;
	}
	
	protected Window makePixmapWindow(String img, int x, int y, int width, int height, boolean scaled) {
		Window w = makeBackgroundWindow(x,y,width,height);
		w.getAppearance().removeAll();
		Pixmap p = makePixmap(img);
		w.getAppearance().add(new PixmapBackground(p, scaled));
		return w;
	}

	protected Window makePixmapWindow(String img, int x, int y, int width, int height) {
		return makePixmapWindow(img, x, y, width, height, false);
	}
	
	protected Window makeBackgroundWindow(int x, int y, int width, int height) {
		Window w = new Window(false,false,false);
		w.getAppearance().removeAll();
		w.removeAllWidgets();
		w.removeWidget(w.getTitleBar());
		w.removeWidget(w.getTitleLabel());
		w.getAppearance().add(new PlainBackground(new Color(0,0,0, 0.3f)));
		w.setPosition(new Point(x,y));
		w.setSize(width, height);
		return w;
	}
	
	protected Window makePlainWindow(int x, int y, int width, int height) {
		if (_plainWindowPixmaps == null) {
			_plainWindowPixmaps = makeSplitPixmapArray("plain");
		}
		
		Window w = new Window(false, false, false);
		w.getAppearance().removeAll();
		w.getAppearance().add(new PixmapBackground(_plainWindowPixmaps[0], _plainWindowPixmaps[1],
				_plainWindowPixmaps[2], _plainWindowPixmaps[3], _plainWindowPixmaps[4], _plainWindowPixmaps[5],
				_plainWindowPixmaps[6], _plainWindowPixmaps[7], _plainWindowPixmaps[8], true));
		w.setPosition(new Point(x, y));
		w.setSize(width, height);
		w.removeAllWidgets();
		return w;
	}
	
	protected Window makeWindow(String title, int x, int y, int width, int height) {
		return makeWindow(title, x, y, width, height, true);
	}
	
	protected Window makeWindow(String title, int x, int y, int width, int height, boolean closeButton) {
		if (_windowPixmaps == null) {
			_windowPixmaps = makeSplitPixmapArray("panel");
		}
		
		if (_titlePixmaps == null) {
			_titlePixmaps = makeSplitPixmapArray("title");
		}
		Window w = new Window(closeButton, false, false);
		w.getAppearance().removeAll();
		w.getAppearance().add(new PixmapBackground(_windowPixmaps[0], _windowPixmaps[1],
				_windowPixmaps[2], _windowPixmaps[3], _windowPixmaps[4], _windowPixmaps[5],
				_windowPixmaps[6], _windowPixmaps[7], _windowPixmaps[8], true));
		w.setPosition(new Point(x, y));
		w.setSize(width, height);
		w.removeAllWidgets();
		
		Label titleBar = new Label("");
		titleBar.getAppearance().removeAll();
		titleBar.getAppearance().add(new PixmapBackground(_titlePixmaps[0], _titlePixmaps[1],
				_titlePixmaps[2], _titlePixmaps[3], _titlePixmaps[4], _titlePixmaps[5],
				_titlePixmaps[6], _titlePixmaps[7], _titlePixmaps[8], true));	
		titleBar.setXY(15, height-40);
		titleBar.setSize(width-40, 26);
		w.addWidget(titleBar);
		
		Label titleLabel = new Label(title);
		titleLabel.getAppearance().removeAll();
		titleLabel.getAppearance().setFont(_fengArial);
		titleLabel.getAppearance().setTextColor(Color.WHITE);
		titleLabel.setXY(25, height-37);
		titleLabel.setSize(width-50, 20);
		w.addWidget(titleLabel);

		if (closeButton) {
			final Window buttonWindow = w;
			Button b = makePixmapButton("PanelClose.png", "PanelClose.png", width-45, height-41, 19, 26);
			b.addButtonPressedListener(new IButtonPressedListener() {
				public void buttonPressed(ButtonPressedEvent e) {
					buttonWindow.close();
				}
			});
			w.addWidget(b);
		}
		w.layout();
		return w;
	}
	
	protected Pixmap[] makeSplitPixmapArray(String prefix) {
		Pixmap[] p = new Pixmap[9];
		p[0] = makePixmap(prefix + "-center.png");
		p[1] = makePixmap(prefix + "-top-left.png");
		p[2] = makePixmap(prefix + "-top.png");
		p[3] = makePixmap(prefix + "-top-right.png");
		p[4] = makePixmap(prefix + "-right.png");
		p[5] = makePixmap(prefix + "-bottom-right.png");
		p[6] = makePixmap(prefix + "-bottom.png");
		p[7] = makePixmap(prefix + "-bottom-left.png");
		p[8] = makePixmap(prefix + "-left.png");
		return p;
	}
	
	protected void widgetAddFocus(ObservableWidget w, final String activeState) {
		w.addFocusListener(new IFocusListener() {
			public void focusChanged(FocusEvent evt) {
				if (evt.isFocusGained()) {
					Main.inst().enableInput(activeState, true);
				}
		
				if (evt.isFocusLost()) {
					Main.inst().enableInput(activeState, false);
				}
			}
		});
	}
	

	@Override
	public void render(float tpf) {
		DisplaySystem.getDisplaySystem().getRenderer().draw(_fengSpatial);
	}

	@Override
	public void update(float tpf) {
		_input.update(tpf);
	}

	@Override
	public void cleanup() {
		if (_mouseListener != null) {
			System.out.println("[cleanup] removing MouseListener " + _mouseListener.getName());
			MouseInput.get().removeListener(_mouseListener);
			_mouseListener = null;
		}
	}
	
	@Override
	public void setInputEnabled(boolean enabled) {
		_input.setEnabled(enabled);
	}
	
	public void setActive(boolean status) {
		super.setActive(status);
		if (!status && _mouseListener != null) {
			System.out.println("[setActive] removing MouseListner " + _mouseListener.getName());
			MouseInput.get().removeListener(_mouseListener);
			_mouseListener = null;
		}
	}
	
	private class MouseListener implements MouseInputListener {

//		private boolean down;
//		private int lastButton;

		private String _name;
		public MouseListener(String name) {
			_name = name;
		}
		
		public String getName() {
			return _name;
		}
		
		public void onButton(int button, boolean pressed, int x, int y) {
			
//			down = pressed;
//			lastButton = button;
			if(pressed)
				_display.fireMousePressedEvent(x, y, getMouseButton(button), 1);
			else
				_display.fireMouseReleasedEvent(x, y, getMouseButton(button), 1);
		}

		public void onMove(int xDelta, int yDelta, int newX, int newY)
		{
			// If the button is down, the mouse is being dragged
//			if(down) {
				//_display.fireMouseDraggedEvent(newX, newY, getMouseButton(lastButton));
//			} else
				_display.fireMouseMovedEvent(newX, newY);
		}

		public void onWheel(int wheelDelta, int x, int y)
		{
			// wheelDelta is positive if the mouse wheel rolls up
			if(wheelDelta > 0)
				_display.fireMouseWheel(x, y, true, wheelDelta);
			else
				_display.fireMouseWheel(x, y, false, wheelDelta);

			// note (johannes): wheeling code not tested on jME, please report problems on www.fenggui.org/forum/
		}

		/**
		 * Helper method that maps the mouse button to the equivalent
		 * FengGUI MouseButton enumeration.
		 * @param button The button pressed or released.
		 * @return The FengGUI MouseButton enumeration matching the
		 * button.
		 */
		private MouseButton getMouseButton(int button)
		{
			switch(button)
			{
			case 0:
				return MouseButton.LEFT;
			case 1:
				return MouseButton.RIGHT;
			case 2:
				return MouseButton.MIDDLE;
			default:
				return MouseButton.LEFT;
			}
		}
	}
	
	private class FengGUISpatial extends Quad {
		private static final long serialVersionUID = 1L;

		@Override
		public void draw(final Renderer r) {

			if (!r.isProcessingQueue()) {
				r.checkAndAdd(this);
				return;
			}

			// drawing FengGUI
			_display.display();

		}

		public void onDraw(final Renderer r) {
			super.onDraw(r);
		}

		@Override
		public void findCollisions(final Spatial scene, final CollisionResults results) {
		}

		@Override
		public void findPick(final Ray toTest, final PickResults results) {
		}

		@Override
		public boolean hasCollision(final Spatial scene, final boolean checkTriangles) {
			return false;
		}

		@Override
		public void setModelBound(final BoundingVolume modelBound) {
		}

		@Override
		public void updateModelBound() {
		}

		@Override
		public int getType() {
			return 0;
		}

		@Override
		public void updateWorldBound() {
		}

	}
}
