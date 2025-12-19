package orange.wz;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class OrangeWzApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(OrangeWzApplication.class)
                .headless(false)
                .run(args);
    }

}
