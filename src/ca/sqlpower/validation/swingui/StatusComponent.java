/*
 * Copyright (c) 2008, SQL Power Group Inc.
 *
 * This file is part of SQL Power Library.
 *
 * SQL Power Library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * SQL Power Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */
package ca.sqlpower.validation.swingui;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.border.Border;

import org.apache.log4j.Logger;

import ca.sqlpower.validation.ValidateResult;

/**
 * A Component that displays the success/failure result
 * with a textual message.
 */
public class StatusComponent extends JLabel {
    
    private static final Logger logger = Logger.getLogger(StatusComponent.class);

    /**
     * The border this component has by default.  If you want a different
     * border when you use it, just call setBorder().
     * 
     */
    private static final Border DEFAULT_BORDER =
        BorderFactory.createEmptyBorder(7, 0, 7, 0);
    
    private ValidateResult result = null;

    /**
     * The icon to show with the message when the status is null or unknown.
     */
    private Icon nullIcon;
    
    /**
     * The icon to show with the message when everything is OK.
     */
    private Icon okIcon;
    
    /**
     * The icon to show with the message when the validation has a warning.
     */
    private Icon warnIcon;
    
    /**
     * The icon to show with the message when the validation has failed.
     */
    private Icon failIcon;
    
    /**
     * Creates a new StatusComponent with no visible display, but
     * which takes up the same amount of space as it would if it
     * was displaying an icon and message.
     */
    public StatusComponent() {
        setOpaque(true);
        setBorder(DEFAULT_BORDER);
        setResult(null);
        
        setNullIcon(new ImageIcon(StatusComponent.class.getClassLoader().getResource("ca/sqlpower/validation/swingui/stat_null_16.png")));
        setOkIcon(getNullIcon());
        setWarnIcon(new ImageIcon(StatusComponent.class.getClassLoader().getResource("ca/sqlpower/validation/swingui/stat_warn_16.png")));
        setFailIcon(new ImageIcon(StatusComponent.class.getClassLoader().getResource("ca/sqlpower/validation/swingui/stat_fail_16.png")));
    }

    public void setResult(ValidateResult error) {
        result = error;

        String text;
        Icon icon;
        if (result == null) {
            text = null;
            icon = getNullIcon();
        } else {
            text = result.getMessage();
            switch(result.getStatus()) {
            case OK:
                icon = getOkIcon();
                break;
            case WARN:
                icon = getWarnIcon();
                break;
            case FAIL:
                icon = getFailIcon();
                break;
            default:
                icon = getNullIcon();
            }
        }
        setText(text);
        setIcon(icon);
    }

    /**
     * Takes the given text and prepends "&lt;html&gt;" before passing to JLabel's
     * setText() method.  If the given string is null or whitespace-only, it is
     * treated as a non-breaking space (to ensure the height of this component won't
     * change when it is used with and without visible text). 
     */
    @Override
    public void setText(String text) {
        if (text == null || text.trim().length() == 0) {
            text = "&nbsp;";
        }
        super.setText("<html>"+text);
    }

    public ValidateResult getResult() {
        return result;
    }

	public Icon getFailIcon() {
		return failIcon;
	}

	public void setFailIcon(Icon failIcon) {
		this.failIcon = failIcon;
	}

	public Icon getNullIcon() {
		return nullIcon;
	}

	public void setNullIcon(Icon nullIcon) {
		this.nullIcon = nullIcon;
	}

	public Icon getOkIcon() {
		return okIcon;
	}

	public void setOkIcon(Icon okIcon) {
		this.okIcon = okIcon;
	}

	public Icon getWarnIcon() {
		return warnIcon;
	}

	public void setWarnIcon(Icon warnIcon) {
		this.warnIcon = warnIcon;
	}
    
    
}
