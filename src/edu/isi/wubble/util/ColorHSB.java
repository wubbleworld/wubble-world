package edu.isi.wubble.util;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

import javax.imageio.ImageIO;

import com.jme.math.FastMath;
import com.jme.renderer.ColorRGBA;

public class ColorHSB {

    private static final long serialVersionUID = 1L;

    public float _hue;
    public float _sat;
    public float _bri;

    public ColorHSB() {
    	_hue = _sat = _bri = 1.0f;
    }
    
    public ColorHSB(ColorRGBA color) {
    	fromColorRGBA(color);
    	clamp();
    }

    public ColorHSB(float hue, float saturation, float brightness) {
    	_hue = hue;
    	_sat = saturation;
    	_bri = brightness;
    	clamp();
    }

    public ColorHSB(ColorHSB hsb) {
        this._hue = hsb._hue;
        this._sat = hsb._sat;
        this._bri = hsb._bri;
        clamp();
    }

    public void set(float hue, float saturation, float brightness) {
    	this._hue = hue;
    	this._sat = saturation;
    	this._bri = brightness;
    	clamp();
    }

    public ColorHSB set(ColorHSB hsb) {
    	if (hsb == null) {
    		_hue = 0;
    		_sat = 0;
    		_bri = 0;
    	} else {
    		_hue = hsb._hue;
    		_sat = hsb._sat;
    		_bri = hsb._bri;
    	}
        return this;
    }

    /**
     * <code>clamp</code> insures that all values are between 0 and 1. If any
     * are less than 0 they are set to zero. If any are more than 1 they are
     * set to one.
     *
     */
    public void clamp() {
    	if (_hue < 0) _hue += 360;
    	if (_hue > 360) _hue -= 360;
    	_sat = FastMath.clamp(_sat, 0f, 1f);
    	_bri = FastMath.clamp(_bri, 0f, 1f);
    }

    /**
     * <code>toString</code> returns the string representation of this color.
     * The format of the string is:<br>
     * @return the string representation of this color.
     */
    public String toString() {
        return "edu.isi.wubble.util: [H="+_hue+", S="+_sat+", B="+_bri+"]";
    }


    public ColorHSB clone() {
        return new ColorHSB(_hue,_sat,_bri);
    }

    /**
     * <code>equals</code> returns true if this color is logically equivalent
     * to a given color. That is, if the values of the two colors are the same.
     * False is returned otherwise.
     * @param o the object to compare againts.
     * @return true if the colors are equal, false otherwise.
     */
    public boolean equals(Object o) {
        if( !(o instanceof ColorHSB) ) {
            return false;
        }

        if(this == o) {
            return true;
        }

        ColorHSB comp = (ColorHSB)o;
        if (Float.compare(_hue, comp._hue) != 0) return false;
        if (Float.compare(_sat, comp._sat) != 0) return false;
        if (Float.compare(_bri, comp._bri) != 0) return false;
        return true;
    }

    public Class getClassTag() {
        return this.getClass();
    }
    
    public void fromColorRGBA(ColorRGBA color) {
    	float max = max(color.r, max(color.g, color.b));
    	float min = min(color.r, min(color.g, color.b));
    	float delta = max - min;
    	
    	_bri = max;
    	if (Float.compare(max,0f) != 0) {
    		_sat = delta / max;
    	}
    	
    	if (Float.compare(_sat, 0) != 0) {
        	float addAngle = 0;
    		if (Float.compare(max, color.r) == 0 && color.g >= color.b) {
    			_hue = (color.g - color.b) / delta;
    		} else if (Float.compare(max, color.r) == 0 && color.g < color.b) {
    			_hue = (color.g - color.b) / delta;
    			addAngle = 360;
    		} else if (Float.compare(max, color.g) == 0) {
    			_hue = (color.b - color.r) / delta;
    			addAngle = 120;
    		} else if (Float.compare(max, color.b) == 0) {
    			_hue = (color.r - color.g) / delta;
    			addAngle = 240;
    		}
        	_hue = (60f * _hue) + addAngle;
    	} else {
    		_hue = 0;
    	}
    }
    
    public ColorRGBA toRGBA() {
    	ColorRGBA color = new ColorRGBA();

    	int h = ((int) Math.floor(_hue / 60.0f)) % 6;
    	float f = (_hue / 60.0f) - (float) h;
    	float p = _bri * (1f - _sat);
    	float q = _bri * (1f - (f*_sat));
    	float t = _bri * (1f - (1f - f)*_sat);
    	
    	if (h == 0)
    		return new ColorRGBA(_bri, t, p, 0);
    	
    	if (h == 1)
    		return new ColorRGBA(q, _bri, p, 0);
    	
    	if (h == 2) 
    		return new ColorRGBA(p, _bri, t, 0);
    	
    	if (h == 3)
    		return new ColorRGBA(p, q, _bri, 0);
    	
    	if (h == 4) 
    		return new ColorRGBA(t, p, _bri, 0);
    	
    	if (h == 5)
    		return new ColorRGBA(_bri, p, q, 0);
    	
    	return color;
    }
    
    public static void main(String[] args) {

    	Comparator<ColorHSB> hue = new Comparator<ColorHSB>() {
			public int compare(ColorHSB arg0, ColorHSB arg1) {
				if (arg0._hue < arg1._hue) 
					return -1;
				else if (arg0._hue > arg1._hue) 
					return 1;
				
				return 0;
			}
    	};
    	
    	Comparator<ColorHSB> bri = new Comparator<ColorHSB>() {
			public int compare(ColorHSB o1, ColorHSB o2) {
				if (o1._bri < o2._bri)
					return -1;
				else if (o1._bri > o2._bri)
					return 1;
				
				if (o1._hue < o2._hue)
					return -1;
				else if (o1._hue > o2._hue)
					return 1;
				
				if (o1._sat < o2._sat)
					return -1;
				else if (o1._sat > o2._sat)
					return 1;
				
				return 0;
			}
    	};
    	
    	LinkedList<ColorHSB> hueColors = new LinkedList<ColorHSB>();
    	LinkedList<ColorHSB> briColors = new LinkedList<ColorHSB>();
    	for (float r = 0; r < 1.01f; r += 0.25f) {
    		for (float g = 0; g < 1.01f; g += 0.25f) {
    			for (float b = 0; b < 1.01f; b += 0.25f) {
    				ColorRGBA blah = new ColorRGBA(r,g,b,0);
    				ColorHSB hsb = new ColorHSB(blah);
    				hueColors.add(hsb);
    				briColors.add(hsb);
    			}
    		}
    	}
    	
    	BufferedImage bi = new BufferedImage(50, 125*20, BufferedImage.TYPE_INT_RGB);
    	Graphics graphics = bi.getGraphics();
    	
    	Collections.sort(hueColors, hue);
    	int count = 0;
    	for (ColorHSB color : hueColors) {
    		ColorRGBA blah = color.toRGBA();
    		System.out.println("color: " + color);
    		graphics.setColor(new Color(blah.r, blah.g, blah.b));
    		graphics.fillRect(0, count*20, 50, 20);
    		++count;
    	}

    	try {
    		ImageIO.write(bi, "png", new File("hue.png"));
    	} catch (Exception e) {
    		e.printStackTrace();
    	}

    	bi = new BufferedImage(50, 125*20, BufferedImage.TYPE_INT_RGB);
    	graphics = bi.getGraphics();
    	
    	Collections.sort(briColors, bri);
    	count = 0;
    	for (ColorHSB color : briColors) {
    		ColorRGBA blah = color.toRGBA();
    		System.out.println("color: " + color);
    		graphics.setColor(new Color(blah.r, blah.g, blah.b));
    		graphics.fillRect(0, count*20, 50, 20);
    		++count;
    	}
    	
    	try {
    		ImageIO.write(bi, "png", new File("bri.png"));
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }
}
