package orange.wz.gui.component.menu;

import orange.wz.gui.MainFrame;
import orange.wz.gui.component.panel.EditPane;

import javax.swing.*;

import static orange.wz.gui.Icons.*;
import static orange.wz.gui.Icons.AiOutlineKey;
import static orange.wz.gui.Icons.AiOutlineReloadIcon;

public class TreeMenu extends JPopupMenu {
    protected JMenuItem btnSave;
    protected JMenuItem btnSaveAs;
    protected JMenuItem btnUnload;
    protected JMenuItem btnReload;
    protected JMenuItem btnChangeKey;
    protected JMenu btnExport;
    protected JMenu btnImport;
    protected JMenu btnSubNode;
    protected JMenu btnSubNodeForList;
    protected JMenuItem btnMoveView;
    protected JMenuItem btnPaste;
    protected JMenuItem btnDelete;
    protected JMenuItem btnCopy;
    protected JMenuItem btnLocalize;
    protected JMenuItem btnImgCompare;
    protected JMenuItem btnOutlink;
    protected JMenuItem btnImgFinder;
    protected JMenuItem btnDelNonCashEqp;
    protected JMenuItem btnOrderAndRename;
    protected JMenuItem btnDelChild;
    protected JMenuItem btnChangeCavFmt;
    protected JMenuItem btnScaleImg;
    protected JMenuItem btnChangeNodeName;
    protected JMenuItem btnChangeIntNodeValue;
    protected JMenuItem btnRawToIcon;
    protected JMenuItem btnChangeCavOrigin;

    public TreeMenu(EditPane editPane) {
        super();

        btnSave = new JMenuItem(MainFrame.i18n.get("tree.menu.save"), AiOutlineSaveIcon);
        btnSave.addActionListener(e -> editPane.save());

        btnSaveAs = new JMenuItem(MainFrame.i18n.get("tree.menu.save_as"), AiOutlineSaveIcon);
        btnSaveAs.addActionListener(e -> editPane.saveAs());

        btnUnload = new JMenuItem(MainFrame.i18n.get("tree.menu.unload"), AiOutlineCloseIcon);
        btnUnload.addActionListener(e -> editPane.unload());

        btnReload = new JMenuItem(MainFrame.i18n.get("tree.menu.reload"), AiOutlineReloadIcon);
        btnReload.addActionListener(e -> editPane.reloadFile());

        btnMoveView = new JMenuItem(MainFrame.i18n.get("tree.menu.move_view"), AiOutlineEye);
        btnMoveView.addActionListener(e -> editPane.move());

        btnPaste = new JMenuItem(MainFrame.i18n.get("tree.menu.paste"), MdOutlineContentPaste);
        btnPaste.addActionListener(e -> editPane.doPaste());

        btnCopy = new JMenuItem(MainFrame.i18n.get("tree.menu.copy"), AiOutlineCopy);
        btnCopy.addActionListener(e -> editPane.doCopy());

        btnDelete = new JMenuItem(MainFrame.i18n.get("tree.menu.delete"), AiOutlineDelete);
        btnDelete.addActionListener(e -> editPane.delete());

        btnChangeKey = new JMenuItem(MainFrame.i18n.get("tree.menu.change_key"), AiOutlineKey);
        btnChangeKey.addActionListener(e -> editPane.changeKey());

        btnExport = new JMenu(MainFrame.i18n.get("tree.menu.export"));
        JMenuItem exportImgBtn = new JMenuItem(MainFrame.i18n.get("tree.menu.img"));
        exportImgBtn.addActionListener(e -> editPane.exportImg());
        btnExport.add(exportImgBtn);
        JMenuItem exportXmlBtn = new JMenuItem(MainFrame.i18n.get("tree.menu.xml"));
        exportXmlBtn.addActionListener(e -> editPane.exportXml());
        btnExport.add(exportXmlBtn);

        btnImport = new JMenu(MainFrame.i18n.get("tree.menu.import"));
        JMenuItem importImgBtn = new JMenuItem(MainFrame.i18n.get("tree.menu.img"));
        importImgBtn.addActionListener(e -> editPane.importImg());
        btnImport.add(importImgBtn);
        JMenuItem importXmlBtn = new JMenuItem(MainFrame.i18n.get("tree.menu.xml"));
        importXmlBtn.addActionListener(e -> editPane.importXml());
        btnImport.add(importXmlBtn);

        btnSubNode = new JMenu(MainFrame.i18n.get("tree.menu.sub_node"));
        btnSubNode.setIcon(AiOutlinePlus);
        JMenuItem addDirBtn = new JMenuItem(MainFrame.i18n.get("tree.menu.directory"));
        addDirBtn.addActionListener(e -> editPane.addWzDirectory());
        btnSubNode.add(addDirBtn);
        JMenuItem addImgBtn = new JMenuItem(MainFrame.i18n.get("tree.menu.image"));
        addImgBtn.addActionListener(e -> editPane.addWzImage());
        btnSubNode.add(addImgBtn);

        btnLocalize = new JMenuItem(MainFrame.i18n.get("tree.menu.localize"));
        btnLocalize.addActionListener(e -> editPane.localizeString());

        btnImgCompare = new JMenuItem(MainFrame.i18n.get("tree.menu.img_compare"));
        btnImgCompare.addActionListener(e -> editPane.compareImg());

        btnOutlink = new JMenuItem(MainFrame.i18n.get("tree.menu.outlink"));
        btnOutlink.addActionListener(e -> editPane.outlink());

        btnImgFinder = new JMenuItem(MainFrame.i18n.get("tree.menu.img_finder"));
        btnImgFinder.addActionListener(e -> editPane.imageFinder());

        btnDelNonCashEqp = new JMenuItem(MainFrame.i18n.get("tree.menu.rm_non_cash_eqp"));
        btnDelNonCashEqp.addActionListener(e -> editPane.removeNonCashEqp());

        btnOrderAndRename = new JMenuItem(MainFrame.i18n.get("tree.menu.order_and_rename"));
        btnOrderAndRename.addActionListener(e -> editPane.sortAndReindexChildren());

        btnDelChild = new JMenuItem(MainFrame.i18n.get("tree.menu.del_child"));
        btnDelChild.addActionListener(e -> editPane.removeAllWzChildWithName());

        btnChangeCavFmt = new JMenuItem(MainFrame.i18n.get("tree.menu.change_cav_fmt"));
        btnChangeCavFmt.addActionListener(e -> editPane.changeCavFmt());

        btnScaleImg = new JMenuItem(MainFrame.i18n.get("tree.menu.scale_img"));
        btnScaleImg.addActionListener(e -> editPane.scaleImage());

        btnChangeNodeName = new JMenuItem(MainFrame.i18n.get("tree.menu.change_node_name"));
        btnChangeNodeName.addActionListener(e -> editPane.changeNodeName());

        btnChangeIntNodeValue = new JMenuItem(MainFrame.i18n.get("tree.menu.change_int_node"));
        btnChangeIntNodeValue.addActionListener(e -> editPane.changeIntNodeValue());

        btnRawToIcon = new JMenuItem(MainFrame.i18n.get("tree.menu.raw_to_icon"));
        btnRawToIcon.addActionListener(e -> editPane.rawToIcon());

        btnChangeCavOrigin = new JMenuItem(MainFrame.i18n.get("tree.menu.change_cav_origin"));
        btnChangeCavOrigin.addActionListener(e -> editPane.changeOriginValue());

        btnSubNodeForList = new JMenu(MainFrame.i18n.get("tree.menu.sub_node_for_list"));
        btnSubNodeForList.setIcon(AiOutlinePlus);
        JMenuItem addCanvasBtn = new JMenuItem(MainFrame.i18n.get("tree.menu.canvas"));
        addCanvasBtn.addActionListener(e -> editPane.addCanvas());
        btnSubNodeForList.add(addCanvasBtn);
        JMenuItem addConvexBtn = new JMenuItem(MainFrame.i18n.get("tree.menu.convex"));
        addConvexBtn.addActionListener(e -> editPane.addConvex());
        btnSubNodeForList.add(addConvexBtn);
        JMenuItem addDoubleBtn = new JMenuItem(MainFrame.i18n.get("tree.menu.double"));
        addDoubleBtn.addActionListener(e -> editPane.addDouble());
        btnSubNodeForList.add(addDoubleBtn);
        JMenuItem addFloatBtn = new JMenuItem(MainFrame.i18n.get("tree.menu.float"));
        addFloatBtn.addActionListener(e -> editPane.addFloat());
        btnSubNodeForList.add(addFloatBtn);
        JMenuItem addIntBtn = new JMenuItem(MainFrame.i18n.get("tree.menu.int"));
        addIntBtn.addActionListener(e -> editPane.addInt());
        btnSubNodeForList.add(addIntBtn);
        JMenuItem addListBtn = new JMenuItem(MainFrame.i18n.get("tree.menu.list"));
        addListBtn.addActionListener(e -> editPane.addList());
        btnSubNodeForList.add(addListBtn);
        JMenuItem addLongBtn = new JMenuItem(MainFrame.i18n.get("tree.menu.long"));
        addLongBtn.addActionListener(e -> editPane.addLong());
        btnSubNodeForList.add(addLongBtn);
        JMenuItem addNullBtn = new JMenuItem(MainFrame.i18n.get("tree.menu.null"));
        addNullBtn.addActionListener(e -> editPane.addNull());
        btnSubNodeForList.add(addNullBtn);
        JMenuItem addShortBtn = new JMenuItem(MainFrame.i18n.get("tree.menu.short"));
        addShortBtn.addActionListener(e -> editPane.addShort());
        btnSubNodeForList.add(addShortBtn);
        JMenuItem addSoundBtn = new JMenuItem(MainFrame.i18n.get("tree.menu.sound"));
        addSoundBtn.addActionListener(e -> editPane.addSound());
        btnSubNodeForList.add(addSoundBtn);
        JMenuItem addStringBtn = new JMenuItem(MainFrame.i18n.get("tree.menu.string"));
        addStringBtn.addActionListener(e -> editPane.addString());
        btnSubNodeForList.add(addStringBtn);
        JMenuItem addUOLBtn = new JMenuItem(MainFrame.i18n.get("tree.menu.uol"));
        addUOLBtn.addActionListener(e -> editPane.addUOL());
        btnSubNodeForList.add(addUOLBtn);
        JMenuItem addVectorBtn = new JMenuItem(MainFrame.i18n.get("tree.menu.vector"));
        addVectorBtn.addActionListener(e -> editPane.addVector());
        btnSubNodeForList.add(addVectorBtn);
    }
}
