package fi.vm.yti.codelist.intake.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.springframework.core.io.ClassPathResource;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public abstract class FileUtils {

    /**
     * Skips the possible BOM character in the beginning of reader.
     *
     * @param reader Reader.
     * @throws IOException
     */
    @SuppressFBWarnings("RR_NOT_CHECKED")
    public static void skipBom(final Reader reader) throws IOException {
        reader.mark(1);
        final char[] possibleBOM = new char[1];
        reader.read(possibleBOM);

        if (possibleBOM[0] != '\ufeff') {
            reader.reset();
        }
    }

    /**
     * Loads a file from classpath inside the application JAR.
     *
     * @param fileName The name of the file to be loaded.
     */
    public static InputStream loadFileFromClassPath(final String fileName) throws IOException {
        final ClassPathResource classPathResource = new ClassPathResource(fileName);
        final InputStream inputStream = classPathResource.getInputStream();
        return inputStream;
    }
}
