package orange.wz.gui.component.menu;

import orange.wz.gui.MainFrame;
import orange.wz.gui.component.panel.EditPane;

import javax.swing.*;

public final class WzImageFileMenu extends TreeMenu {

    public WzImageFileMenu(EditPane editPane) {
        super(editPane);

        JMenu btnExport = new JMenu(MainFrame.i18n.get("tree.menu.export"));
        JMenuItem exportXmlBtn = new JMenuItem(MainFrame.i18n.get("tree.menu.xml"));
        exportXmlBtn.addActionListener(e -> editPane.exportXml());
        btnExport.add(exportXmlBtn);

        add(btnSubNodeForList);
        add(btnSave);
        add(btnSaveAs);
        add(btnUnload);
        add(btnReload);
        add(btnMoveView);
        add(btnCopy);
        add(btnPaste);
        add(btnChangeKey);
        add(btnExport);
        add(btnLocalize);
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
