package orange.wz.gui.component.form;

import orange.wz.gui.MainFrame;
import orange.wz.gui.component.form.data.*;
import orange.wz.gui.form.data.*;
import orange.wz.gui.utils.JMessageUtil;
import orange.wz.provider.WzDirectory;
import orange.wz.provider.WzImage;
import orange.wz.provider.WzObject;
import orange.wz.provider.properties.*;

import javax.swing.*;

public class FormSaveHandler {
    private static final MainFrame mainFrame = MainFrame.getInstance();

    public static void saveClick() {
        WzObject wzObject = mainFrame.getCurWzObject();

        if (wzObject == null) return;

        boolean res = switch (wzObject.getType()) {
            case FOLDER, WZ_FILE, PNG_PROPERTY, RAW_DATA_PROPERTY -> false;
            case DIRECTORY -> changeDir((WzDirectory) wzObject);
            case IMAGE -> changeImg((WzImage) wzObject);
            case CANVAS_PROPERTY -> changeCanvas((WzCanvasProperty) wzObject);
            case CONVEX_PROPERTY -> changeConvex((WzConvexProperty) wzObject);
            case DOUBLE_PROPERTY -> changeDouble((WzDoubleProperty) wzObject);
            case FLOAT_PROPERTY -> changeFloat((WzFloatProperty) wzObject);
            case INT_PROPERTY -> changeInt((WzIntProperty) wzObject);
            case LIST_PROPERTY -> changeList((WzListProperty) wzObject);
            case LONG_PROPERTY -> changeLong((WzLongProperty) wzObject);
            case NULL_PROPERTY -> changeNull((WzNullProperty) wzObject);
            case SHORT_PROPERTY -> changeShort((WzShortProperty) wzObject);
            case SOUND_PROPERTY -> changeSound((WzSoundProperty) wzObject);
            case STRING_PROPERTY -> changeString((WzStringProperty) wzObject);
            case UOL_PROPERTY -> changeUol((WzUOLProperty) wzObject);
            case VECTOR_PROPERTY -> changeVector((WzVectorProperty) wzObject);
        };

        JTree tree = mainFrame.getTree();
        tree.updateUI();

        if (!res) {
            JMessageUtil.warn("什么都没有保存");
        }
    }

    private static boolean changeDir(WzDirectory directory) {
        String newName = mainFrame.getNodeForm().getData().getName();
        if (newName.equals(directory.getName())) return false;

        if (directory.isWzFile()) {
            if (newName.endsWith(".wz")) {
                directory.setName(newName);
                directory.getWzFile().setName(newName);
            } else {
                JMessageUtil.error("不是 .wz 结尾");
                return false;
            }
        } else {
            directory.setName(newName);
        }
        directory.setTempChanged(true);
        return true;
    }

    private static boolean changeImg(WzImage image) {
        String newName = mainFrame.getNodeForm().getData().getName();
        if (newName.equals(image.getName())) return false;

        if (newName.endsWith(".img")) {
            image.setName(newName);
        } else {
            JMessageUtil.error("不是 .img 结尾");
            return false;
        }

        image.setTempChanged(true);
        return true;
    }

    private static boolean changeCanvas(WzCanvasProperty property) {
        CanvasFormData data = mainFrame.getCanvasForm().getData();

        property.setName(data.getName());
        property.setPng(data.getValue(), data.getFormat());

        property.getWzImage().setChanged(true);
        property.setTempChanged(true);
        return true;
    }

    private static boolean changeConvex(WzConvexProperty property) {
        String newName = mainFrame.getNodeForm().getData().getName();
        if (newName.equals(property.getName())) return false;

        property.setName(newName);

        property.getWzImage().setChanged(true);
        property.setTempChanged(true);
        return true;
    }

    private static boolean changeDouble(WzDoubleProperty property) {
        DoubleFormData data = mainFrame.getDoubleForm().getData();

        property.setName(data.getName());
        property.setValue(data.getValue());

        property.getWzImage().setChanged(true);
        property.setTempChanged(true);
        return true;
    }

    private static boolean changeFloat(WzFloatProperty property) {
        FloatFormData data = mainFrame.getFloatForm().getData();

        property.setName(data.getName());
        property.setValue(data.getValue());

        property.getWzImage().setChanged(true);
        property.setTempChanged(true);
        return true;
    }

    private static boolean changeInt(WzIntProperty property) {
        IntFormData data = mainFrame.getIntForm().getData();

        property.setName(data.getName());
        property.setValue(data.getValue());

        property.getWzImage().setChanged(true);
        property.setTempChanged(true);
        return true;
    }

    private static boolean changeList(WzListProperty property) {
        String newName = mainFrame.getNodeForm().getData().getName();

        property.setName(newName);

        property.getWzImage().setChanged(true);
        property.setTempChanged(true);
        return true;
    }

    private static boolean changeLong(WzLongProperty property) {
        LongFormData data = mainFrame.getLongForm().getData();

        property.setName(data.getName());
        property.setValue(data.getValue());

        property.getWzImage().setChanged(true);
        property.setTempChanged(true);
        return true;
    }

    private static boolean changeNull(WzNullProperty property) {
        String newName = mainFrame.getNodeForm().getData().getName();
        if (property.getName().equals(newName)) return false;

        property.setName(newName);

        property.getWzImage().setChanged(true);
        property.setTempChanged(true);
        return true;
    }

    private static boolean changeShort(WzShortProperty property) {
        ShortFormData data = mainFrame.getShortForm().getData();

        property.setName(data.getName());
        property.setValue(data.getValue());

        property.getWzImage().setChanged(true);
        property.setTempChanged(true);
        return true;
    }

    private static boolean changeSound(WzSoundProperty property) {
        SoundFormData data = mainFrame.getSoundForm().getData();

        property.setName(data.getName());
        property.setSound(data.getSoundBytes());

        property.getWzImage().setChanged(true);
        property.setTempChanged(true);
        return true;
    }

    private static boolean changeString(WzStringProperty property) {
        StringFormData data = mainFrame.getStringForm().getData();

        property.setName(data.getName());
        property.setValue(data.getValue());

        property.getWzImage().setChanged(true);
        property.setTempChanged(true);
        return true;
    }

    private static boolean changeUol(WzUOLProperty property) {
        StringFormData data = null;

        WzObject target = property.getUolTarget();
        if (target instanceof WzCanvasProperty) {
            data = mainFrame.getUolCanvasForm().getUolData();
        } else if (target instanceof WzSoundProperty) {
            data = mainFrame.getUolSoundForm().getUolData();
        }

        if (data == null) return false;

        property.setName(data.getName());
        property.setValue(data.getValue());

        property.getWzImage().setChanged(true);
        property.setTempChanged(true);
        return true;
    }

    private static boolean changeVector(WzVectorProperty property) {
        VectorFormData data = mainFrame.getVectorForm().getData();

        property.setName(data.getName());
        property.setX(data.getX());
        property.setY(data.getY());

        property.getWzImage().setChanged(true);
        property.setTempChanged(true);
        return true;
    }
}
