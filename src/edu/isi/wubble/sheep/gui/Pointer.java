package edu.isi.wubble.sheep.gui;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

import org.fenggui.Label;
import org.fenggui.background.PixmapBackground;
import org.fenggui.render.Binding;
import org.fenggui.render.Pixmap;

import com.jme.math.FastMath;
import com.jme.util.resource.ResourceLocatorTool;

import edu.isi.wubble.Constants;

public class Pointer extends Label {
	private Integer _team;
	
	// This is in integer degrees now -> [0,360)
	private int _rotation;
	
	// Eventually we can drop the pixmaps entirely
	private static BufferedImage _masterBlue;
	private static BufferedImage _masterRed;
	private static Pixmap[] _bluePix; 
	private static Pixmap[] _redPix;
	private static PixmapBackground[] _blueBG;
	private static PixmapBackground[] _redBG;
	
	// Idea: make 360 x 2 pixmaps and store them
	static {
		_bluePix = new Pixmap[360];
		_redPix = new Pixmap[360];
		
		_blueBG = new PixmapBackground[360];
		_redBG = new PixmapBackground[360];
		
		URL	blueUrl = ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_TEXTURE, "pointerBlue.png");
		URL redUrl = ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_TEXTURE, "pointerRed.png");
		
		try {
			_masterBlue = ImageIO.read(blueUrl);
			_masterRed = ImageIO.read(redUrl);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		initImages(_bluePix, _blueBG, _masterBlue);
		initImages(_redPix, _redBG, _masterRed);
	}
	
	public static void initImages(Pixmap[] pix, PixmapBackground[] bg, BufferedImage master) {
		for (int r = 0; r < 360; r++) {
			double rot = r * FastMath.DEG_TO_RAD;
			double 	sin = Math.abs(Math.sin(rot)), 
					cos = Math.abs(Math.cos(rot));
	        int w = master.getWidth(),
	        	h = master.getHeight();
	        int newW = (int) Math.floor(w * cos + h * sin),
	        	newH = (int) Math.floor(h * cos + w * sin);
			
			BufferedImage image = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
			
			Graphics2D g = (Graphics2D) image.getGraphics();
			g.translate((newW - w) / 2, (newH - h) / 2);
	        g.rotate(rot, w/2, h/2);
			g.drawRenderedImage(master, null);
			g.dispose();
			
			pix[r] = new Pixmap(Binding.getInstance().getTexture(image));
			bg[r] = new PixmapBackground(pix[r], false); 
		}	
	}
	
	public Pointer(Integer team) {
		super();
		
		_team = team;
		
		_rotation = 0;
		
		this.setSize(21, 21);
		
		updateBackground();
	}

	public void setRotation(float rot) {
		// what range does rot have? -2pi to 2pi?
		_rotation = ((int) Math.floor(rot * FastMath.RAD_TO_DEG) + 360) % 360;
		
		updateBackground();
	}

	// Can we avoid making a new background every time? Does it matter?
	private void updateBackground() {
		this.getAppearance().removeAll();

		if (_team.equals(Constants.BLUE_TEAM)) {
			this.getAppearance().add(_blueBG[_rotation]);
//			_bg = new PixmapBackground(_bluePix[_rotation], false);
		} else {
			this.getAppearance().add(_redBG[_rotation]);
//			_bg = new PixmapBackground(_redPix[_rotation], false);
		}
	}
}
