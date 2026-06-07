package orange.wz.gui.component.menu;

import orange.wz.gui.component.panel.EditPane;

import javax.swing.*;

public final class WzValuePropertyMenu extends TreeMenu {
    public WzValuePropertyMenu(EditPane editPane) {
        super(editPane);

        add(btnCopy);
        add(btnDelete);
        add(btnLocalize);
    }

    public JMenuItem getBtnDelete() {
        return btnDelete;
    }

    public JMenuItem getBtnCopy() {
        return btnCopy;
    }
}
