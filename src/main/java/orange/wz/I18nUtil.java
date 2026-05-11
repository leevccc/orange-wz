package orange.wz;

import jakarta.annotation.PostConstruct;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

@Component
public class I18nUtil {
    private MessageSource messageSource;

    private Locale locale = Locale.CHINA;

    @PostConstruct
    public void init() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");
        this.messageSource = messageSource;
        this.locale = loadLocaleFromIni();
    }

    public String get(String code, Object... args) {
        return messageSource.getMessage(
                code,
                args,
                locale
        );
    }

    private Locale loadLocaleFromIni() {
        Path path = Path.of("config.ini");

        if (!Files.exists(path)) {
            return Locale.CHINA;
        }

        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            properties.load(in);
            String language = properties.getProperty("language", "zh_CN");

            return Locale.forLanguageTag(
                    language.replace('_', '-')
            );

        } catch (IOException e) {
            e.printStackTrace();

            return Locale.CHINA;
        }
    }
}
