package AiStudyHub.BE.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
@Slf4j
public class TextExtractionUtil {

    private final Tika tika;

    public TextExtractionUtil() {
        this.tika = new Tika();
        this.tika.setMaxStringLength(-1); // No limit for extracted text length
    }

    public String extractText(InputStream inputStream) {
        try {
            return tika.parseToString(inputStream);
        } catch (Exception e) {
            log.error("Failed to extract text from document: {}", e.getMessage());
            return "";
        }
    }
}
