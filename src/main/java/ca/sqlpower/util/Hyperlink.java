package ca.sqlpower.util;

/**
 * The Hyperlink class represents a link with text and a hypertext
 * reference.  It does not use the java.net.URL class internally,
 * because it is intended that the href attribute will often be
 * formatted using the LongMessageFormat class.
 */
public class Hyperlink {
	protected String text;
	protected String href;

	public Hyperlink(String text, String href) {
		this.text=text;
		this.href=href;
	}

	
	/**
	 * Gets the value of text
	 *
	 * @return the value of text
	 */
	public String getText() {
		return this.text;
	}

	/**
	 * Sets the value of text
	 *
	 * @param argText Value to assign to this.text
	 */
	public void setText(String argText){
		this.text = argText;
	}

	/**
	 * Gets the value of href
	 *
	 * @return the value of href
	 */
	public String getHref() {
		return this.href;
	}

	/**
	 * Sets the value of href
	 *
	 * @param argHref Value to assign to this.href
	 */
	public void setHref(String argHref){
		this.href = argHref;
	}

}
