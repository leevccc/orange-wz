package orange.wz.gui.component.menu;

import orange.wz.gui.MainFrame;
import orange.wz.gui.component.panel.EditPane;

import javax.swing.*;

public final class WzXmlFileMenu extends TreeMenu {
    public WzXmlFileMenu(EditPane editPane) {
        super(editPane);

        JMenu btnExport = new JMenu(MainFrame.i18n.get("tree.menu.export"));
        JMenuItem exportImgBtn = new JMenuItem(MainFrame.i18n.get("tree.menu.img"));
        exportImgBtn.addActionListener(e -> editPane.exportImg());
        btnExport.add(exportImgBtn);

        add(btnSubNodeForList);
        add(btnSave);
        add(btnSaveAs);
        add(btnUnload);
        add(btnReload);
        add(btnMoveView);
        add(btnCopy);
        add(btnPaste);
        add(btnExport);
        add(btnImgCompare);
        add(btnImgFinder);
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

    public JMenuItem getBtnCopy() {
        return btnCopy;
    }
}
