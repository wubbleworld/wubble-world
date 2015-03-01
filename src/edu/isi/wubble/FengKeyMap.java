package edu.isi.wubble;

import java.util.TreeMap;
import org.fenggui.event.Key;
import org.lwjgl.input.Keyboard;

public class FengKeyMap {

	protected TreeMap<Integer, Key> map = null;
	protected static FengKeyMap fengKeyMap = null;
	
	private FengKeyMap() {
		fillTree();
	}
	
	public static FengKeyMap inst() {
		if (fengKeyMap == null) {
			fengKeyMap = new FengKeyMap();
		}
		return fengKeyMap;
	}
		
	/**
	 * Helper method that maps LWJGL key events to FengGUI.
	 * @return The Key enumeration of the last key pressed.
	 */
	private void fillTree() {
		map = new TreeMap<Integer, Key>();
		map.put(new Integer(Keyboard.KEY_BACK), Key.BACKSPACE);
		map.put(new Integer(Keyboard.KEY_RETURN), Key.ENTER);
		map.put(new Integer(Keyboard.KEY_DELETE), Key.DELETE);
		map.put(new Integer(Keyboard.KEY_UP), Key.UP);
		map.put(new Integer(Keyboard.KEY_RIGHT), Key.RIGHT);
		map.put(new Integer(Keyboard.KEY_LEFT), Key.LEFT);
		map.put(new Integer(Keyboard.KEY_DOWN), Key.DOWN);
		map.put(new Integer(Keyboard.KEY_TAB), Key.TAB);

		map.put(new Integer(Keyboard.KEY_SCROLL), Key.SHIFT);
		map.put(new Integer(Keyboard.KEY_LMENU), Key.ALT);
		map.put(new Integer(Keyboard.KEY_RMENU), Key.ALT);
		map.put(new Integer(Keyboard.KEY_LCONTROL), Key.CTRL);
		map.put(new Integer(Keyboard.KEY_RCONTROL), Key.CTRL);
		map.put(new Integer(Keyboard.KEY_RSHIFT), Key.SHIFT);
		map.put(new Integer(Keyboard.KEY_LSHIFT), Key.SHIFT);
		map.put(new Integer(Keyboard.KEY_INSERT), Key.INSERT);

		map.put(new Integer(Keyboard.KEY_F12), Key.F12);
		map.put(new Integer(Keyboard.KEY_F11), Key.F11);
		map.put(new Integer(Keyboard.KEY_F10), Key.F10);
		map.put(new Integer(Keyboard.KEY_F9), Key.F9);
		map.put(new Integer(Keyboard.KEY_F8), Key.F8);
		map.put(new Integer(Keyboard.KEY_F7), Key.F7);
		map.put(new Integer(Keyboard.KEY_F6), Key.F6);
		map.put(new Integer(Keyboard.KEY_F5), Key.F5);
		map.put(new Integer(Keyboard.KEY_F4), Key.F4);
		map.put(new Integer(Keyboard.KEY_F3), Key.F3);
		map.put(new Integer(Keyboard.KEY_F2), Key.F2);
		map.put(new Integer(Keyboard.KEY_F1), Key.F1);
	}
	
	public Key getMapping() {
		Key mapping = map.get(new Integer(Keyboard.getEventKey()));
		if (mapping == null) {
			if ("1234567890".indexOf(Keyboard.getEventCharacter()) != -1) {
				mapping = Key.DIGIT;
			} else {
				mapping = Key.LETTER;
			}
		}
		return mapping;
	}

}
