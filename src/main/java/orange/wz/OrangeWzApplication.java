package orange.wz;

import orange.wz.gui.MainFrame;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.swing.*;
import java.util.List;
import java.util.Objects;

@SpringBootApplication
public class OrangeWzApplication {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = MainFrame.getInstance();
            frame.setIconImages(List.of(Objects.requireNonNull(MainFrame.loadImage("logo512.png"))));
            frame.setVisible(true);
        });

        SpringApplication.run(OrangeWzApplication.class, args);
    }

}
