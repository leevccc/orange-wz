package orange.wz.gui.filter;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public final class HexFilter extends DocumentFilter {
    @Override
    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
        if (isValidHex(fb.getDocument().getText(0, fb.getDocument().getLength()) + string)) {
            super.insertString(fb, offset, string.toUpperCase(), attr);
        }
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
        String current = fb.getDocument().getText(0, fb.getDocument().getLength());
        String before = current.substring(0, offset);
        String after = current.substring(offset + length);
        String newText = before + (text == null ? "" : text) + after;

        if (isValidHex(newText)) {
            super.replace(fb, offset, length, text.toUpperCase(), attrs);
        }
    }

    private boolean isValidHex(String text) {
        return text.length() <= 2 && text.matches("[0-9A-Fa-f]*");
    }
}
