package fi.vm.yti.cls.intake.data;

import com.google.common.base.Stopwatch;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.vm.yti.cls.common.model.PostManagementDistrict;
import fi.vm.yti.cls.common.model.PostalCode;
import fi.vm.yti.cls.common.model.StreetAddress;
import fi.vm.yti.cls.common.model.StreetNumber;
import fi.vm.yti.cls.common.model.UpdateStatus;
import fi.vm.yti.cls.intake.domain.Domain;
import fi.vm.yti.cls.intake.domain.DomainConstants;
import fi.vm.yti.cls.intake.parser.PostManagementDistrictParser;
import fi.vm.yti.cls.intake.parser.PostalCodeParser;
import fi.vm.yti.cls.intake.parser.StreetAddressParser;
import fi.vm.yti.cls.intake.update.UpdateManager;
import fi.vm.yti.cls.intake.util.FileUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Class that provides access to Posti HTTP service for related PostalCode data.
 */
@Service
public class PostiDataAccess implements DataAccess {

    private static final Logger LOG = LoggerFactory.getLogger(PostiDataAccess.class);
    private static final String POSTI_DATA_LISTING_URL = "http://www.posti.fi/webpcode/";
    private static final String LOCAL_POSTI_DATA_DIR = "/data/cls/cls-intake/posti/";
    private static final String POSTALCODE_FILE_PREFIX = "PCF_";
    private static final String STREETADDRESS_FILE_PREFIX = "BAF_";
    private static final String DEFAULT_POSTIDATAFILE_DIR = "/postalcodes";
    private static final String DEFAULT_POSTIADDRESSDATAFILE_DIR = "/streetaddresses";
    private static final String DEFAULT_POSTIDATAFILE_NAME = "PCF_20170427.dat";
    private static final String DEFAULT_POSTISTREETADDRESSDATAFILE_NAME = "BAF_20170610.dat";
    private UpdateManager updateManager;
    private Domain domain;
    private PostalCodeParser postalCodeParser;
    private StreetAddressParser streetAddressParser;
    private PostManagementDistrictParser postManagementDistrictParser;

    @Inject
    public PostiDataAccess(final UpdateManager updateManager,
                           final Domain domain,
                           final StreetAddressParser streetAddressParser,
                           final PostalCodeParser postalCodeParser,
                           final PostManagementDistrictParser postManagementDistrictParser) {
        this.updateManager = updateManager;
        this.streetAddressParser = streetAddressParser;
        this.postalCodeParser = postalCodeParser;
        this.postManagementDistrictParser = postManagementDistrictParser;
        this.domain = domain;
    }

    /**
     * Method that ensures initialization and refreshing of Posti Data.
     */
    public void initializeOrRefresh() {
        checkForNewData();
    }

    /**
     * Checks Posti HTTP Service for new data.
     *
     * If new Data is found, it is fethched and processed.
     */
    public boolean checkForNewData() {
        boolean reIndex = false;
        checkForNewFile(POSTALCODE_FILE_PREFIX);
        checkForNewFile(STREETADDRESS_FILE_PREFIX);
        final String latestVersion = getLatestPostiDataFileName(POSTALCODE_FILE_PREFIX);
        final String latestStreetDataVersion = getLatestPostiDataFileName(STREETADDRESS_FILE_PREFIX);

        if (updateManager.shouldUpdateData(DomainConstants.DATA_POSTALCODES, latestVersion)) {
            loadPostCodes(getCurrentPostiData());
            reIndex = true;
        } else {
            LOG.info("PostalCodes already up to date, skipping...");
        }

        if (updateManager.shouldUpdateData(DomainConstants.DATA_POSTMANAGEMENTDISTRICTS, latestVersion)) {
            loadPostManagementDistricts(getCurrentPostiData());
            reIndex = true;
        } else {
            LOG.info("PostManagementDistricts already up to date, skipping...");
        }

        if (updateManager.shouldUpdateData(DomainConstants.DATA_POSTALCODE_POSTMANAGEMENTDISTRICT_RELATIONS, latestVersion)) {
            ensurePostalCodePostManagementDistrictRelation(getCurrentPostiData());
            reIndex = true;
        } else {
            LOG.info("PostalCode and PostManagementDistrict relations already up to date, skipping...");
        }

        if (updateManager.shouldUpdateData(DomainConstants.DATA_STREETADDRESSES, latestStreetDataVersion)) {
            loadStreetAddresses(getCurrentStreetAddressData());
            loadStreetNumbers(getCurrentStreetAddressData());
            reIndex = true;
        } else {
            LOG.info("StreetAddresses already up to date, skipping...");
        }
        return reIndex;
    }

    /**
     * Checks Posti HTTP for the latest data file, and downloads it if it does not yet exist locally.
     */
    private void checkForNewFile(final String prefix) {
        final String latestFile = getLatestFile(prefix);

        if (latestFile != null) {
            final String[] parts = resolveFileNameFromUrl(latestFile).split("[.]");
            if (parts.length > 0) {
                final String latestFileName = parts[0] + ".dat";
                final String latestStoredFile = getLatestPostiDataFileName(prefix);

                if (!latestFileName.equals(latestStoredFile)) {
                    LOG.info("Downloading new Posti data file: " + latestFileName);
                    final boolean downloadSuccess = downloadFile(latestFile);

                    if (!downloadSuccess) {
                        LOG.error("Downloading new Posti data file failed.");
                    }
                }
            }
        }
    }

    /**
     * Resolves the latest Posti Data file and returns it as an InputStream.
     *
     * @return The latest Posti Data as an InputStream.
     */
    private InputStream getCurrentPostiData() {
        InputStream inputStream = null;
        // Try to use latest Posti Data file.
        try {
            final File file = getLatestPostiDataFile(POSTALCODE_FILE_PREFIX);
            if (file != null) {
                inputStream = new FileInputStream(file);
            }
        } catch (FileNotFoundException e) {
            LOG.info("No locally stored Posti files found, the default Postal Code file is latest, using it...");
        }
        // Use default posti data file as backup.
        if (inputStream == null) {
            try {
                inputStream = FileUtils.loadFileFromClassPath(DEFAULT_POSTIDATAFILE_DIR + "/" + DEFAULT_POSTIDATAFILE_NAME);
            } catch (IOException e) {
                LOG.error("Default Postal Code file fetching failed: " + e.getMessage());
            }
        }
        return inputStream;
    }

    /**
     * Resolves the latest Posti Data file and returns it as an InputStream.
     *
     * @return The latest Posti Data as an InputStream.
     */
    private InputStream getCurrentStreetAddressData() {
        InputStream inputStream = null;

        // Try to use latest Posti Street Address Data file.
        try {
            final File file = getLatestPostiDataFile(STREETADDRESS_FILE_PREFIX);
            if (file != null) {
                inputStream = new FileInputStream(file);
            }
        } catch (FileNotFoundException e) {
            LOG.info("No locally stored Posti Street Address files found, the default Postal Code file is latest, using it...");
        }

        // Use default posti data file as backup.
        if (inputStream == null) {
            try {
                inputStream = FileUtils.loadFileFromClassPath(DEFAULT_POSTIADDRESSDATAFILE_DIR + "/" + DEFAULT_POSTISTREETADDRESSDATAFILE_NAME);
            } catch (IOException e) {
                LOG.error("Default Street Address file fetching failed: " + e.getMessage());
            }
        }
        return inputStream;
    }

    /**
     * Get the latest HTTP address corresponding to the file prefix from the Posti HTTP Server.
     *
     * @param filePrefix Prefix of the filename to choose from the server.
     * @return An HTTP URL that matches the prefix, null if no matching file is found.
     */
    private String getLatestFile(final String filePrefix) {
        final List<String> list = getZipFileListing(filePrefix);
        if (!list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }

    /**
     * Download a file from the Posti HTTP Server and store it to disk.
     *
     * @param file An URL of the file to download.
     * @return True if downloading is successful, false if it fails.
     */
    private boolean downloadFile(final String file) {
        final List<String> files = new ArrayList<>();
        files.add(file);
        return storeFilesToDisk(files);
    }

    /**
     * Get file listing from the Posti HTTP server.
     *
     * @return List of ZIP-files available from the server.
     */
    private List<String> getZipFileListing(final String prefix) {
        final List<String> list = new ArrayList<>();
        try {
            final Document doc = Jsoup.connect(POSTI_DATA_LISTING_URL).get();
            final Elements links = doc.select("a[href]");
            for (final Element link : links) {
                final String href = link.attr("href");
                final String fileName = resolveFileNameFromUrl(href);
                if (fileName.startsWith(prefix) && href.endsWith(".zip") && !href.contains("/arch")) {
                    list.add(href);
                }
            }
        } catch (IOException e) {
            LOG.error("Connnecting to Posti HTTP service failed: " + e.getMessage());
        }
        return list;
    }

    /**
     * Stores files to disk from an Posti HTTP Service.
     *
     * @param files List of files available from the HTTP Service.
     * @return Returns true if files stored successfully, false if not.
     */
    @SuppressFBWarnings("DMI_HARDCODED_ABSOLUTE_FILENAME")
    private boolean storeFilesToDisk(final List<String> files) {
        try {
            Files.createDirectories(Paths.get(LOCAL_POSTI_DATA_DIR));
            files.forEach(remoteFile -> {
                final String localFilePath = LOCAL_POSTI_DATA_DIR + resolveFileNameFromUrl(remoteFile);
                final File localFile = new File(localFilePath);
                if (!localFile.exists()) {
                    LOG.info("Storing new Posti data file to: " + localFilePath);
                    OutputStream outputStream = null;
                    try {
                        outputStream = new BufferedOutputStream(new FileOutputStream(localFile));
                        final InputStream inputStream = new URL(remoteFile).openStream();
                        IOUtils.copy(inputStream, outputStream);
                        outputStream.close();

                        LOG.info(remoteFile + " downloaded successfully.");
                        unzipFile(localFile.toPath());

                    } catch (IOException e) {
                        LOG.error("IOException storing file: " + e.getMessage());
                    }
                } else {
                    LOG.info("Local file already exists: " + localFilePath + ", no download.");
                }
            });
        } catch (IOException e) {
            LOG.error("Error while storing files: " + e.getMessage());
            return false;
        }
        return true;
    }

    private String resolveFileNameFromUrl(final String url) {
        return url.substring(url.lastIndexOf('/') + 1);
    }

    /**
     * Unzips the file from the given path to a local folder.
     *
     * @param filePath The path of the zip-file.
     */
    private void unzipFile(final Path filePath) throws IOException {
        LOG.info("Unzipping file: " + filePath.toString());
        ZipEntry zipEntry;
        FileInputStream fileInputStream = null;
        ZipInputStream zipInputStream = null;
        FileOutputStream fileOutputStream = null;

        try {
            fileInputStream = new FileInputStream(filePath.toFile());
            zipInputStream = new ZipInputStream(new BufferedInputStream(fileInputStream));

            while ((zipEntry = zipInputStream.getNextEntry()) != null){
                final byte[] tmp = new byte[4*1024];

                final String opFilePath = LOCAL_POSTI_DATA_DIR + zipEntry.getName();
                fileOutputStream = new FileOutputStream(opFilePath);

                LOG.info("Extracting file to " + opFilePath);

                int size = 0;

                while ((size = zipInputStream.read(tmp)) != -1) {
                    fileOutputStream.write(tmp, 0, size);
                }
                fileOutputStream.flush();
            }
        } catch (FileNotFoundException e) {
            LOG.error("File " + filePath.toString() + " not found: " + e.getMessage());
        } catch (IOException e) {
            LOG.error("Unzipping file " + filePath.toString() + " failed: " + e.getMessage());
        } finally {
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
            if (zipInputStream != null) {
                zipInputStream.close();
            }
        }
    }

    /**
     * Returns the latest stored Posti data file name.
     *
     * @return Latest Posti data file filename.
     */
    private String getLatestPostiDataFileName(final String prefix) {
        final File latestLocalFile = getLatestPostiDataFile(prefix);
        if (latestLocalFile != null) {
            return latestLocalFile.getName();
        }
        if (prefix.equals(POSTALCODE_FILE_PREFIX)) {
            return DEFAULT_POSTIDATAFILE_NAME;
        } else {
            return DEFAULT_POSTISTREETADDRESSDATAFILE_NAME;
        }
    }

    /**
     * Does a lookup from the filesystem for postalcode DAT files files.
     *
     * @return The latest Posti data file from the filesystem.
     */
    @SuppressFBWarnings("DMI_HARDCODED_ABSOLUTE_FILENAME")
    private File getLatestPostiDataFile(final String prefix) {
        if (Files.isDirectory(Paths.get(LOCAL_POSTI_DATA_DIR))) {
            try {
                final Iterator<Path> paths = Files.walk(Paths.get(LOCAL_POSTI_DATA_DIR)).sorted(Collections.reverseOrder()).iterator();
                while (paths.hasNext()) {
                    final Path filePath = paths.next();
                    final String fileName = filePath.toFile().getName();
                    if (Files.isRegularFile(filePath) && fileName.startsWith(prefix) && fileName.endsWith(".dat")) {
                        return filePath.toFile();
                    }
                }
            } catch (IOException e) {
                LOG.error("Local file lookup failed: " + e.getMessage());
                return null;
            }
        }
        LOG.error("No post code file found from disk, consider using jar embedded data.");
        return null;
    }

    /**
     * Parses and persists Street Addresses from data.
     *
     * @param inputStream Posti street address data as InputStream.
     */
    private void loadStreetAddresses(final InputStream inputStream) {
        LOG.info("Loading street addresses...");
        final UpdateStatus updateStatus = updateManager.createStatus(DomainConstants.DATA_STREETADDRESSES, DomainConstants.SOURCE_POSTI, getLatestPostiDataFileName(STREETADDRESS_FILE_PREFIX), UpdateManager.UPDATE_RUNNING);
        final Stopwatch watch = Stopwatch.createStarted();
        final List<StreetAddress> streetAddresses = streetAddressParser.parseStreetAddressesFromInputStream(DomainConstants.SOURCE_POSTI, inputStream);
        LOG.info("Street Address data loaded: " + streetAddresses.size() + " streetaddresses found in " + watch);
        watch.reset().start();
        final List<List<StreetAddress>> chunks = ListUtils.partition(streetAddresses, 10000);
        chunks.parallelStream().forEach(chunk -> domain.persistStreetAddresses(chunk));
        LOG.info("StreetAddress data persisted in: " + watch);
        if (updateStatus.getStatus().equals(UpdateManager.UPDATE_RUNNING)) {
            updateManager.updateSuccessStatus(updateStatus);
        }
    }

    /**
     * Parses, persists and associates Street Numbers from data.
     *
     * @param inputStream Posti street address data as InputStream.
     */
    private void loadStreetNumbers(final InputStream inputStream) {
        LOG.info("Loading street numbers...");
        final UpdateStatus updateStatus = updateManager.createStatus(DomainConstants.DATA_STREETNUMBERS, DomainConstants.SOURCE_POSTI, getLatestPostiDataFileName(STREETADDRESS_FILE_PREFIX), UpdateManager.UPDATE_RUNNING);
        final Stopwatch watch = Stopwatch.createStarted();
        final List<StreetNumber> streetNumbers = streetAddressParser.parseStreetNumbersFromInputStream(DomainConstants.SOURCE_POSTI, inputStream);
        LOG.info("Street Number data loaded: " + streetNumbers.size() + " streetnumbers found in " + watch);
        watch.reset().start();
        final List<List<StreetNumber>> chunks = ListUtils.partition(streetNumbers, 10000);
        chunks.parallelStream().forEach(chunk -> domain.persistStreetNumbers(chunk));
        LOG.info("StreetAddress data persisted in: " + watch);
        if (updateStatus.getStatus().equals(UpdateManager.UPDATE_RUNNING)) {
            updateManager.updateSuccessStatus(updateStatus);
        }
    }

    /**
     * Parses and persists Postal Codes from data.
     *
     * @param inputStream Posti data as InputStream.
     */
    private void loadPostCodes(final InputStream inputStream) {
        LOG.info("Loading postalcodes...");
        final UpdateStatus updateStatus = updateManager.createStatus(DomainConstants.DATA_POSTALCODES, DomainConstants.SOURCE_POSTI, getLatestPostiDataFileName(POSTALCODE_FILE_PREFIX), UpdateManager.UPDATE_RUNNING);
        final Stopwatch watch = Stopwatch.createStarted();
        final List<PostalCode> postalCodes = postalCodeParser.parsePostalCodesFromInputStream(DomainConstants.SOURCE_POSTI, inputStream);
        LOG.info("PostalCode data loaded: " + postalCodes.size() + " postalcodes found in " + watch);
        watch.reset().start();
        domain.persistPostalCodes(postalCodes);
        LOG.info("PostalCode data persisted in: " + watch);

        if (updateStatus.getStatus().equals(UpdateManager.UPDATE_RUNNING)) {
            updateManager.updateSuccessStatus(updateStatus);
        }
    }

    /**
     * Parses and persists PostManagementDistricts from data.
     *
     * @param inputStream Posti data as InputStream.
     */
    private void loadPostManagementDistricts(final InputStream inputStream) {
        LOG.info("Loading postmanagementdistricts...");
        final UpdateStatus updateStatus = updateManager.createStatus(DomainConstants.DATA_POSTMANAGEMENTDISTRICTS, DomainConstants.SOURCE_POSTI, getLatestPostiDataFileName(POSTALCODE_FILE_PREFIX), UpdateManager.UPDATE_RUNNING);
        final Stopwatch watch = Stopwatch.createStarted();
        final List<PostManagementDistrict> postManagementDistricts = postManagementDistrictParser.parsePostManagementDistrictsFromInputStream(DomainConstants.SOURCE_POSTI, inputStream);
        LOG.info("PostManagementDistrict data loaded: " + postManagementDistricts.size() + " postmanagementdistricts found in " + watch);
        watch.reset().start();
        domain.persistPostManagementDistricts(postManagementDistricts);
        LOG.info("PostManagementDistrict data persisted in " + watch);

        if (updateStatus.getStatus().equals(UpdateManager.UPDATE_RUNNING)) {
            updateManager.updateSuccessStatus(updateStatus);
        }
    }

    /**
     * Ensures PostalCode and PostManagementDistrict relations.
     *
     * @param inputStream Posti data as InputStream.
     */
    private void ensurePostalCodePostManagementDistrictRelation(final InputStream inputStream) {
        LOG.info("Resolving PostalCode and PostManagementDistrict relations...");
        final UpdateStatus updateStatus = updateManager.createStatus(DomainConstants.DATA_POSTALCODE_POSTMANAGEMENTDISTRICT_RELATIONS, DomainConstants.SOURCE_POSTI, getLatestPostiDataFileName(POSTALCODE_FILE_PREFIX), UpdateManager.UPDATE_RUNNING);
        final Stopwatch watch = Stopwatch.createStarted();
        postManagementDistrictParser.ensurePostalCodePostManagementDistrictRelations(inputStream);
        LOG.info("PostalCode and PostManagementDistrict relations resolved and stored in " + watch);
        if (updateStatus.getStatus().equals(UpdateManager.UPDATE_RUNNING)) {
            updateManager.updateSuccessStatus(updateStatus);
        }
    }

}
