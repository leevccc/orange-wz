package orange.wz.manager;

import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.NonNull;
import orange.wz.gui.MainFrame;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.util.List;
import java.util.Objects;

@Component
public class ServerManager implements ApplicationContextAware, ApplicationRunner, DisposableBean {
    @Getter
    private static ApplicationContext context;
    @Getter
    private static String version;

    @Value("${version}")
    public void setVersion(String v) {
        version = v;
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext ctx) throws BeansException {
        context = ctx;
    }

    @Override
    public void run(@Nullable ApplicationArguments args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = MainFrame.getInstance();
            frame.setIconImages(List.of(Objects.requireNonNull(MainFrame.loadImage("logo512.png"))));
            frame.setVisible(true);
        });
    }

    @Override
    public void destroy() {
    }

    public static <T> T getBean(Class<T> clazz) {
        return context.getBean(clazz);
    }
}
