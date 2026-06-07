package orange.wz.gui.component.menu;

import orange.wz.gui.component.panel.EditPane;

import javax.swing.*;

public final class WzDirectoryMenu extends TreeMenu {
    public WzDirectoryMenu(EditPane editPane) {
        super(editPane);

        add(btnSubNode);
        add(btnCopy);
        add(btnPaste);
        add(btnDelete);
        add(btnImgFinder);
        add(btnImport);
        add(btnImgCompare);
        add(btnDelNonCashEqp);
    }

    public JMenuItem getBtnPaste() {
        return btnPaste;
    }

    public JMenuItem getBtnDelete() {
        return btnDelete;
    }

    public JMenuItem getBtnCopy() {
        return btnCopy;
    }
}
