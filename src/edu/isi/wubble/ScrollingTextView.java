package edu.isi.wubble;

import org.fenggui.ITextWidget;
import org.fenggui.ScrollContainer;
import org.fenggui.text.TextRun;
import org.fenggui.text.TextStyle;
import org.fenggui.text.TextView;

public class ScrollingTextView extends ScrollContainer implements ITextWidget {

	private TextView textView;
	
	public ScrollingTextView() {
		textView = new TextView();
		setInnerWidget(textView);
		setSize(10, 10);
		
	}
	
	public TextView getTextView() {
		return textView;
	}

	/**
	 * @return the text editor's text
	 */
	public String getText()	{
		return textView.getText();
	}

	/**
	 * Define the textEditor's text
	 * 
	 * @param text
	 *            Text to set
	 */
	public void setText(String text) {
		textView.setText(text);
		layout();
	}

	/**
	 * Append text to the end of the textEditor
	 * 
	 * @param text
	 *            Text to append
	 */
	public void appendText(String text) {
		textView.appendText(text);
		layout();
	}

	/**
	 * Append text to the end of the textEditor
	 * 
	 * @param text
	 *            Text to append
	 */
	public void appendText(String text, TextStyle ts) {
		textView.appendText(text, ts);
		layout();
	}
	
	public void appendText(TextRun run) {
		textView.appendText(run);
		layout();
	}
	
	/**
	 * Terminate the current line by writing the line separator string and
	 * Append text to the end of the textEditor
	 * 
	 * @param text
	 *            Text to append
	 */
	public void addTextLine(String text) {
		textView.addTextLine(text);
		layout();
	}
	
	/**
	 * Terminate the current line by writing the line separator string and
	 * Append text to the end of the textEditor
	 * 
	 * @param text
	 *            Text to append
	 */
	public void addTextLine(String text, TextStyle ts) {
		textView.addTextLine(text, ts);
		layout();
	}
	
}
