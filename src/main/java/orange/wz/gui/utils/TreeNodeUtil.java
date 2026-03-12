package orange.wz.gui.utils;

import orange.wz.provider.*;
import orange.wz.provider.properties.WzListProperty;
import orange.wz.provider.properties.WzStringProperty;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.List;

public final class TreeNodeUtil {
    public static List<String> getNodePathWithoutRoot(DefaultMutableTreeNode node) {
        TreeNode[] nodePaths = node.getPath();
        List<String> paths = new ArrayList<>();

        for (int i = 1; i < nodePaths.length; i++) { // 跳过 root
            DefaultMutableTreeNode n = (DefaultMutableTreeNode) nodePaths[i];
            paths.add(((WzObject) n.getUserObject()).getName());
        }
        return paths;
    }

    public static String getNpcActionName(DefaultMutableTreeNode node) {
        String result = null;
        WzStringProperty wzObject = (WzStringProperty) node.getUserObject();
        String actionMark = wzObject.getName();
        if (actionMark.matches("d\\d+")) return "对话框";

        String[] paths = wzObject.getPath().split("/");
        String first = paths[0];
        if (first.endsWith(".img")) {
            String name = paths[1];
            String npcId = String.format("%07d.img", Integer.parseInt(name));

            for (int i = 1; i < paths.length; i++) {
                node = (DefaultMutableTreeNode) node.getParent();
            }

            node = (DefaultMutableTreeNode) node.getParent(); // String
            if (node == null) return null;

            node = (DefaultMutableTreeNode) node.getParent();
            if (node == null) return null;

            DefaultMutableTreeNode cNode;
            WzFolder npcFolder = null;
            for (int i = 0; i < node.getChildCount(); i++) {
                cNode = (DefaultMutableTreeNode) node.getChildAt(i);
                if (cNode.getUserObject() instanceof WzFolder cFolder && cFolder.getName().equals("Npc")) {
                    npcFolder = cFolder;
                    break;
                }
            }
            if (npcFolder == null) return null;

            WzImage npcImage = null;
            for (WzObject obj : npcFolder.getChildren()) {
                if (obj.getName().equals(npcId)) {
                    npcImage = (WzImage) obj;
                    break;
                }
            }
            if (npcImage == null) return null;

            npcImage.parse();
            WzStringProperty stringProperty = findStringProperty(npcImage, actionMark);
            if (stringProperty == null) return null;
            WzListProperty speak = (WzListProperty) stringProperty.getParent();
            if (speak == null) return null;
            WzListProperty action = (WzListProperty) speak.getParent();
            if (action == null) return null;
            result = action.getName();
        } else if (first.equals("String.wz")) {
            String name = paths[2];
            String npcId = String.format("%07d.img", Integer.parseInt(name));

            for (int i = 1; i < paths.length; i++) {
                node = (DefaultMutableTreeNode) node.getParent();
            }

            node = (DefaultMutableTreeNode) node.getParent();
            if (node == null) return null;

            DefaultMutableTreeNode cNode;
            WzDirectory npcDir = null;
            for (int i = 0; i < node.getChildCount(); i++) {
                cNode = (DefaultMutableTreeNode) node.getChildAt(i);
                if (cNode.getUserObject() instanceof WzDirectory cDir && cDir.getName().equals("Npc.wz")) {
                    npcDir = cDir;
                    break;
                }
            }
            if (npcDir == null) return null;
            npcDir.getWzFile().parse();

            WzImage npcImage = npcDir.getImage(npcId);
            if (npcImage == null) return null;

            npcImage.parse();
            WzStringProperty stringProperty = findStringProperty(npcImage, actionMark);
            if (stringProperty == null) return null;
            WzListProperty speak = (WzListProperty) stringProperty.getParent();
            if (speak == null) return null;
            WzListProperty action = (WzListProperty) speak.getParent();
            if (action == null) return null;
            result = action.getName();
        }

        if (result == null) return null;
        if (result.equals("info")) return "默认";
        return result;
    }

    private static WzStringProperty findStringProperty(WzImage npcImage, String target) {
        for (WzObject child : npcImage.getChildren()) {
            WzStringProperty result = findInNode(child, target);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private static WzStringProperty findInNode(WzObject node, String target) {
        if (node instanceof WzStringProperty strProp) {
            if (target.equals(strProp.getValue())) {
                return strProp;
            }
        }

        if (node instanceof WzListProperty listProp) {
            for (WzObject child : listProp.getChildren()) {
                WzStringProperty result = findInNode(child, target);
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }
}
