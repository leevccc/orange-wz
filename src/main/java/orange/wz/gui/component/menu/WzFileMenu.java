package orange.wz.gui.component.menu;

import orange.wz.gui.component.panel.EditPane;

import javax.swing.*;

public final class WzFileMenu extends TreeMenu {
    public WzFileMenu(EditPane editPane) {
        super(editPane);

        add(btnSubNode);
        add(btnSave);
        add(btnSaveAs);
        add(btnUnload);
        add(btnReload);
        add(btnMoveView);
        add(btnPaste);
        add(btnChangeKey);
        add(btnExport);
        add(btnImport);
        add(btnLocalize);
        add(btnImgCompare);
        add(btnOutlink);
    }

    public JMenuItem getBtnPaste() {
        return btnPaste;
    }
}
