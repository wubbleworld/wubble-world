package edu.isi.wubble.sheep.gui;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

import javax.imageio.ImageIO;

import org.fenggui.Label;
import org.fenggui.background.PixmapBackground;
import org.fenggui.composites.Window;
import org.fenggui.layout.Alignment;
import org.fenggui.render.Binding;
import org.fenggui.render.Pixmap;
import org.fenggui.util.Color;
import org.fenggui.util.Point;

import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.util.resource.ResourceLocatorTool;

import edu.isi.wubble.Main;
import edu.isi.wubble.gamestates.GUIGameState;

public class Map extends Window {
	public HashMap<String, Pointer> _wubblePointers = new HashMap<String, Pointer>();
	public HashMap<String, Label> _wubbleNames = new HashMap<String, Label>();
	
	public Label _redScore, _blueScore;
	
	public Map() {
		super();
		this.removeAllWidgets();
		this.getAppearance().removeAll();
		this.getAppearance().removeAll();
		
		try {
			URL mapURL = ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_TEXTURE, "mapPanel.png");
			BufferedImage mapImage;
			
			mapImage = ImageIO.read(mapURL);
			
			Pixmap mapPix = new Pixmap(Binding.getInstance().getTexture(mapImage));
			PixmapBackground mapBG = new PixmapBackground(mapPix, true);
			
			this.getAppearance().add(mapBG);
			this.setSize((int) (mapImage.getWidth()), (int) (mapImage.getHeight()));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		_redScore = new Label("0");
		_redScore.setSize(40, 20);
		_redScore.setPosition(new Point(72,32));
		_redScore.getAppearance().setFont(GUIGameState._fengArialBold);
		_redScore.getAppearance().setTextColor(Color.WHITE);
		_redScore.getAppearance().setAlignment(Alignment.RIGHT);
		
		_blueScore = new Label("0");
		_blueScore.setSize(40, 20);
		_blueScore.setPosition(new Point(123,32));
		_blueScore.getAppearance().setFont(GUIGameState._fengArialBold);
		_blueScore.getAppearance().setTextColor(Color.WHITE);
		_blueScore.getAppearance().setAlignment(Alignment.LEFT);
		
		this.addWidget(_redScore);
		this.addWidget(_blueScore);
	}
	
	public void addWubble(String name, Point pos) {
		Point namePos = new Point(pos.getX(), pos.getY());
		namePos.translate(20, 5);
		Label nameLabel = new Label(name);
		nameLabel.setWidth(100);
		nameLabel.setPosition(namePos);
		nameLabel.getAppearance().setFont(GUIGameState._fengArialBold);
		nameLabel.getAppearance().setTextColor(Color.WHITE);
		
		Pointer pointer = new Pointer(Main.inst().getTeam());
		pointer.setPosition(pos);
		
		_wubbleNames.put(name, nameLabel);
		_wubblePointers.put(name, pointer);
		
		this.addWidget(nameLabel);
		this.addWidget(pointer);
	}
	
	public void deleteWubble(String name) {
		this.removeWidget(_wubbleNames.get(name));
		_wubbleNames.remove(name);
		this.removeWidget(_wubblePointers.get(name));
		_wubblePointers.remove(name);
	}
	
	// This takes a position in normal game coordinates
	public void updateWubble(String name, Vector3f pos, Quaternion rot) {
		Pointer pointer = _wubblePointers.get(name);
		
		// This happens if the wubble is on the other team
		if (pointer == null) {
			return;
		}
		
		Point p = convertPos(pos);
		
		pointer.setPosition(p);
		pointer.setRotation(convertRot(rot));
		
		Label nameLabel = _wubbleNames.get(name);
		
		// name should go on the left side
		if (p.getX() > (this.getWidth() / 2.0f)) {
			p.translate(-3 - nameLabel.getWidth(), 0);
			nameLabel.setPosition(p);
			nameLabel.getAppearance().setAlignment(Alignment.RIGHT);
		} else {
			p.translate(20, 5);
			nameLabel.setPosition(p);
			nameLabel.getAppearance().setAlignment(Alignment.LEFT);
		}
	}
	
	private Point convertPos(Vector3f pos) {
		int left = 30;
		int bottom = this.getHeight() - 370;
		int width = 175;
		int height = 330;
		
		// World values are [-21,+21] -> [-42,0]
		float x = left + Math.abs(((pos.getX() - 21.0f) / 42f) * (float) width);
		// World values are [-42,+42] -> [84,0]
		float y = bottom + height - Math.abs(((pos.getZ() - 42.0f) / 84f) * (float) height);
		
		return new Point((int) x, (int) y);
	}
	
	private float convertRot(Quaternion rot) {
		Vector3f axis = new Vector3f();
		float angle = rot.toAngleAxis(axis);
		
		if (axis.y < 0) {
			angle = FastMath.TWO_PI - angle;
		}

//		if (angle > FastMath.PI) {
//			angle -= FastMath.PI;
//		} else {
//			angle += FastMath.PI;
//		}
		
		return -angle;
	}

	public void updateScore(int blueScore, int redScore) {
		_blueScore.setText(String.valueOf(blueScore));
		_redScore.setText(String.valueOf(redScore));
	}
}
