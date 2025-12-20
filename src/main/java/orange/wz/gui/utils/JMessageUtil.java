package orange.wz.gui.utils;

import orange.wz.gui.MainFrame;

import javax.swing.*;
import java.awt.*;

public class JMessageUtil {

    public static void info(String message) {
        info("消息", message);
    }

    public static void info(String title, String message) {
        info(MainFrame.getInstance(), title, message);
    }

    public static void info(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(
                parent,
                message,
                title,
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    public static void warn(String message) {
        warn("警告", message);
    }

    public static void warn(String title, String message) {
        warn(MainFrame.getInstance(), title, message);
    }

    public static void warn(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(
                parent,
                message,
                title,
                JOptionPane.WARNING_MESSAGE
        );
    }

    public static void error(String message) {
        error("错误", message);
    }

    public static void error(String title, String message) {
        error(MainFrame.getInstance(), title, message);
    }

    public static void error(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(
                parent,
                message,
                title,
                JOptionPane.ERROR_MESSAGE
        );
    }
}
