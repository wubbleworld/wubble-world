package edu.isi.wubble.character;

import java.util.*;

import com.jme.animation.AnimationController;
import com.jme.animation.BoneAnimation;
import com.jme.scene.Controller;

public class AnimationEngine extends Controller {
	
	private static final long serialVersionUID = 1L;

	private AnimationController ac;
	protected HashMap<String,AnimationObject> map;
	
	protected AnimationObject activeAnimation;
	
	public AnimationEngine(AnimationController ac) {
		this.ac = ac;
		map = new HashMap<String,AnimationObject>();
	}
	
	public void addAnimation(String name, int startFrame, int endFrame, String nextAnim) {
		AnimationObject ao = new AnimationObject(name, startFrame, endFrame, nextAnim);
		
		map.put(name, ao);
	}
	
	public void playAnimation(String name) {
		activeAnimation = map.get(name);
		
		if (ac.getActiveAnimation() != null) {
			ac.setCurrentFrame(activeAnimation.getStartFrame());
		} 
	}
	
	public void update(float tpf) {
		if (ac.getActiveAnimation() != null) {
			BoneAnimation anim = ac.getActiveAnimation();
			int frame = anim.getCurrentFrame();
			if (anim.hasChildren()) {
				anim.getSubanimation(0);
				frame = anim.getSubanimation(0).getCurrentFrame();
			}
			if (frame > activeAnimation.getEndFrame()) {
				playAnimation(activeAnimation.getNext());
			}
		}		
	}
}

class AnimationObject {
	
	private String name;
	private int startFrame;
	private int endFrame;
	private String next;
	
	public AnimationObject(String name, int start, int end, String next) {
		this.name = name;
		this.startFrame = start;
		this.endFrame = end;
		this.next = next;
	}
	
	public String getName() {
		return name;
	}
	
	public int getStartFrame() {
		return startFrame;
	}
	
	public int getEndFrame() {
		return endFrame;
	}

	public String getNext() {
		return next;
	}
}