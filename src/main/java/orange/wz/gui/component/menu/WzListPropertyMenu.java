package orange.wz.gui.component.menu;

import orange.wz.gui.component.panel.EditPane;

import javax.swing.*;

public final class WzListPropertyMenu extends TreeMenu {
    public WzListPropertyMenu(EditPane editPane) {
        super(editPane);

        add(btnSubNodeForList);
        add(btnCopy);
        add(btnPaste);
        add(btnDelete);
        add(btnLocalize);
        add(btnImgCompare);
        add(btnImgFinder);
        add(btnOutlink);
        add(btnOrderAndRename);
        add(btnDelChild);
        add(btnChangeCavFmt);
        add(btnScaleImg);
        add(btnChangeNodeName);
        add(btnChangeIntNodeValue);
        add(btnRawToIcon);
        add(btnChangeCavOrigin);
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
