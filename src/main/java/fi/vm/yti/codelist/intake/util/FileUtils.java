package fi.vm.yti.codelist.intake.util;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.ClassPathResource;

public interface FileUtils {

    static InputStream loadFileFromClassPath(final String fileName) throws IOException {
        final ClassPathResource classPathResource = new ClassPathResource(fileName);
        return classPathResource.getInputStream();
    }
}
