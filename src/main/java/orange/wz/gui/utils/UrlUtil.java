package orange.wz.gui.utils;

import lombok.extern.slf4j.Slf4j;
import orange.wz.gui.MainFrame;

import java.awt.*;
import java.io.IOException;
import java.net.URI;

import static java.awt.Desktop.*;

@Slf4j
public final class UrlUtil {
    public static void open(String url) {
        if (!isDesktopSupported()) {
            JMessageUtil.warn(MainFrame.i18n.get("warn.system_not_supported"));
            return;
        }

        Desktop desktop = getDesktop();
        if (!desktop.isSupported(Action.BROWSE)) {
            JMessageUtil.warn(MainFrame.i18n.get("warn.system_not_supported"));
            return;
        }

        try {
            desktop.browse(URI.create(url));
        } catch (IOException ex) {
            JMessageUtil.error(MainFrame.i18n.get("error.open_url"));
        }
    }
}
