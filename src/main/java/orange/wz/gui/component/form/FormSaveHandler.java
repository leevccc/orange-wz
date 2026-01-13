package orange.wz.gui.component.form;

import orange.wz.gui.component.form.data.*;
import orange.wz.gui.component.panel.EditPane;
import orange.wz.gui.utils.JMessageUtil;
import orange.wz.provider.WzDirectory;
import orange.wz.provider.WzImage;
import orange.wz.provider.WzObject;
import orange.wz.provider.properties.*;

public class FormSaveHandler {
    public static void saveClick(WzObject wzObject, EditPane editPane) {
        if (wzObject == null) return;

        boolean res = switch (wzObject.getType()) {
            case FOLDER, WZ_FILE, PNG_PROPERTY, RAW_DATA_PROPERTY -> false;
            case DIRECTORY -> changeDir((WzDirectory) wzObject, editPane);
            case IMAGE -> changeImg((WzImage) wzObject, editPane);
            case CANVAS_PROPERTY -> changeCanvas((WzCanvasProperty) wzObject, editPane);
            case CONVEX_PROPERTY -> changeConvex((WzConvexProperty) wzObject, editPane);
            case DOUBLE_PROPERTY -> changeDouble((WzDoubleProperty) wzObject, editPane);
            case FLOAT_PROPERTY -> changeFloat((WzFloatProperty) wzObject, editPane);
            case INT_PROPERTY -> changeInt((WzIntProperty) wzObject, editPane);
            case LIST_PROPERTY -> changeList((WzListProperty) wzObject, editPane);
            case LONG_PROPERTY -> changeLong((WzLongProperty) wzObject, editPane);
            case NULL_PROPERTY -> changeNull((WzNullProperty) wzObject, editPane);
            case SHORT_PROPERTY -> changeShort((WzShortProperty) wzObject, editPane);
            case SOUND_PROPERTY -> changeSound((WzSoundProperty) wzObject, editPane);
            case STRING_PROPERTY -> changeString((WzStringProperty) wzObject, editPane);
            case UOL_PROPERTY -> changeUol((WzUOLProperty) wzObject, editPane);
            case VECTOR_PROPERTY -> changeVector((WzVectorProperty) wzObject, editPane);
        };

        editPane.getTree().updateUI();

        if (!res) {
            JMessageUtil.warn("什么都没有保存");
        }
    }

    private static boolean changeDir(WzDirectory directory, EditPane editPane) {
        String newName = editPane.getNodeForm().getData().getName();
        if (newName.equals(directory.getName())) return false;

        if (directory.isWzFile()) {
            if (newName.endsWith(".wz")) {
                directory.setNameAnyway(newName);
                directory.getWzFile().setNameAnyway(newName);
            } else {
                JMessageUtil.error("不是 .wz 结尾");
                return false;
            }
        } else if (!directory.setName(newName)) {
            JMessageUtil.error("存在同名节点，保存失败");
            return false;
        }
        directory.setTempChanged(true);
        return true;
    }

    private static boolean changeImg(WzImage image, EditPane editPane) {
        String newName = editPane.getNodeForm().getData().getName();
        if (newName.equals(image.getName())) return false;

        if (!newName.endsWith(".img")) {
            JMessageUtil.error("不是 .img 结尾");
            return false;
        } else if (!image.setName(newName)) {
            JMessageUtil.error("存在同名节点，保存失败");
            return false;
        }

        image.setTempChanged(true);
        return true;
    }

    private static boolean changeCanvas(WzCanvasProperty property, EditPane editPane) {
        CanvasFormData data = editPane.getCanvasForm().getData();

        if (!property.getName().equals(data.getName()) && !property.setName(data.getName())) {
            JMessageUtil.error("存在同名节点，保存失败");
            return false;
        }

        property.setPng(data.getValue(), data.getFormat(), data.getScale());

        property.getWzImage().setChanged(true);
        property.setTempChanged(true);
        return true;
    }

    private static boolean changeConvex(WzConvexProperty property, EditPane editPane) {
        String newName = editPane.getNodeForm().getData().getName();
        if (newName.equals(property.getName())) return false;

        if (!property.setName(newName)) {
            JMessageUtil.error("存在同名节点，保存失败");
            return false;
        }

        property.getWzImage().setChanged(true);
        property.setTempChanged(true);
        return true;
    }

    private static boolean changeDouble(WzDoubleProperty property, EditPane editPane) {
        DoubleFormData data = editPane.getDoubleForm().getData();

        if (!property.getName().equals(data.getName()) && !property.setName(data.getName())) {
            JMessageUtil.error("存在同名节点，保存失败");
            return false;
        }
        property.setValue(data.getValue());

        property.getWzImage().setChanged(true);
        property.setTempChanged(true);
        return true;
    }

    private static boolean changeFloat(WzFloatProperty property, EditPane editPane) {
        FloatFormData data = editPane.getFloatForm().getData();

        if (!property.getName().equals(data.getName()) && !property.setName(data.getName())) {
            JMessageUtil.error("存在同名节点，保存失败");
            return false;
        }
        property.setValue(data.getValue());

        property.getWzImage().setChanged(true);
        property.setTempChanged(true);
        return true;
    }

    private static boolean changeInt(WzIntProperty property, EditPane editPane) {
        IntFormData data = editPane.getIntForm().getData();

        if (!property.getName().equals(data.getName()) && !property.setName(data.getName())) {
            JMessageUtil.error("存在同名节点，保存失败");
            return false;
        }
        property.setValue(data.getValue());

        property.getWzImage().setChanged(true);
        property.setTempChanged(true);
        return true;
    }

    private static boolean changeList(WzListProperty property, EditPane editPane) {
        String newName = editPane.getNodeForm().getData().getName();

        if (!property.getName().equals(newName) && !property.setName(newName)) {
            JMessageUtil.error("存在同名节点，保存失败");
            return false;
        }

        property.getWzImage().setChanged(true);
        property.setTempChanged(true);
        return true;
    }

    private static boolean changeLong(WzLongProperty property, EditPane editPane) {
        LongFormData data = editPane.getLongForm().getData();

        if (!property.getName().equals(data.getName()) && !property.setName(data.getName())) {
            JMessageUtil.error("存在同名节点，保存失败");
            return false;
        }
        property.setValue(data.getValue());

        property.getWzImage().setChanged(true);
        property.setTempChanged(true);
        return true;
    }

    private static boolean changeNull(WzNullProperty property, EditPane editPane) {
        String newName = editPane.getNodeForm().getData().getName();
        if (property.getName().equals(newName)) return false;

        if (!property.setName(newName)) {
            JMessageUtil.error("存在同名节点，保存失败");
            return false;
        }

        property.getWzImage().setChanged(true);
        property.setTempChanged(true);
        return true;
    }

    private static boolean changeShort(WzShortProperty property, EditPane editPane) {
        ShortFormData data = editPane.getShortForm().getData();

        if (!property.getName().equals(data.getName()) && !property.setName(data.getName())) {
            JMessageUtil.error("存在同名节点，保存失败");
            return false;
        }
        property.setValue(data.getValue());

        property.getWzImage().setChanged(true);
        property.setTempChanged(true);
        return true;
    }

    private static boolean changeSound(WzSoundProperty property, EditPane editPane) {
        SoundFormData data = editPane.getSoundForm().getData();

        if (!property.getName().equals(data.getName()) && !property.setName(data.getName())) {
            JMessageUtil.error("存在同名节点，保存失败");
            return false;
        }
        property.setSound(data.getSoundBytes());

        property.getWzImage().setChanged(true);
        property.setTempChanged(true);
        return true;
    }

    private static boolean changeString(WzStringProperty property, EditPane editPane) {
        StringFormData data = editPane.getStringForm().getData();

        if (!property.getName().equals(data.getName()) && !property.setName(data.getName())) {
            JMessageUtil.error("存在同名节点，保存失败");
            return false;
        }
        property.setValue(data.getValue());

        property.getWzImage().setChanged(true);
        property.setTempChanged(true);
        return true;
    }

    private static boolean changeUol(WzUOLProperty property, EditPane editPane) {
        StringFormData data = null;

        WzObject target = property.getUolTarget();
        if (target instanceof WzCanvasProperty) {
            data = editPane.getUolCanvasForm().getUolData();
        } else if (target instanceof WzSoundProperty) {
            data = editPane.getUolSoundForm().getUolData();
        }

        if (data == null) return false;

        if (!property.getName().equals(data.getName()) && !property.setName(data.getName())) {
            JMessageUtil.error("存在同名节点，保存失败");
            return false;
        }
        property.setValue(data.getValue());

        property.getWzImage().setChanged(true);
        property.setTempChanged(true);
        return true;
    }

    private static boolean changeVector(WzVectorProperty property, EditPane editPane) {
        VectorFormData data = editPane.getVectorForm().getData();

        if (!property.getName().equals(data.getName()) && !property.setName(data.getName())) {
            JMessageUtil.error("存在同名节点，保存失败");
            return false;
        }
        property.setX(data.getX());
        property.setY(data.getY());

        property.getWzImage().setChanged(true);
        property.setTempChanged(true);
        return true;
    }
}
