package fi.vm.yti.cls.intake.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;
import fi.vm.yti.cls.common.model.BusinessId;
import fi.vm.yti.cls.common.model.UpdateStatus;
import fi.vm.yti.cls.intake.domain.Domain;
import fi.vm.yti.cls.intake.domain.DomainConstants;
import fi.vm.yti.cls.intake.parser.BusinessIdParser;
import fi.vm.yti.cls.intake.update.UpdateManager;
import fi.vm.yti.cls.intake.util.Utils;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Implementing class for YTJDataAccess interface.
 *
 * This class provides method implementations for accessing source data from PRH / YTJ AvoinData.
 */
@Service
public class YtjDataAccess implements DataAccess {

    private static final Logger LOG = LoggerFactory.getLogger(YtjDataAccess.class);
    private static final int YTJ_PAGESIZE = 1000;
    private static final int MAX_RETRIES = 5;
    // TODO: Consider refactoring, get base dump elsewhere and play catchup afterwards with setting this date accordingly.
    private static final String HISTORY_START_FROM = "2017-01-01";
    private static final String YTJ_API = "http://avoindata.prh.fi/bis/v1?totalResults={totalResults}&maxResults={pageSize}&resultsFrom={resultsFrom}&companyRegistrationFrom={companyRegistrationFrom}";
    private final Domain domain;
    private final UpdateManager updateManager;
    private final BusinessIdParser businessIdParser;

    @Inject
    public YtjDataAccess(final Domain domain,
                         final UpdateManager updateManager,
                         final BusinessIdParser businessIdParser) {
        this.domain = domain;
        this.updateManager = updateManager;
        this.businessIdParser = businessIdParser;
    }

    /**
     * Loads Business Id information from PRH YTJ API and persists them to database.
     */
    private void loadBusinessIds(final String companyRegistrationFrom) {
        final Stopwatch watch = Stopwatch.createStarted();
        final UpdateStatus updateStatus = updateManager.createStatus(DomainConstants.DATA_BUSINESSIDS, DomainConstants.SOURCE_YTJ, companyRegistrationFrom, Utils.todayInIso8601(), UpdateManager.UPDATE_RUNNING);
        LOG.info("Loading businessids...");
        fetchBusinessIds(YTJ_PAGESIZE, 0, companyRegistrationFrom, updateStatus);
        LOG.info("BusinessId data loaded in " + watch);
    }

    private void fetchBusinessIds(final int pageSize,
                                  final int resultsFrom,
                                  final String companyRegistrationFrom,
                                  final UpdateStatus updateStatus) {
        fetchBusinessIds(pageSize, resultsFrom, companyRegistrationFrom, 0, updateStatus);
    }

    private void fetchBusinessIds(final int pageSize,
                                  final int resultsFrom,
                                  final String companyRegistrationFrom,
                                  final int retryCount,
                                  final UpdateStatus updateStatus) {
        LOG.info("Loading businessids with pageSize: " + pageSize + " from: " + resultsFrom + " with company registration date from: " + companyRegistrationFrom);
        final Stopwatch watch = Stopwatch.createStarted();
        boolean retry = false;

        try {

            final HttpClient httpClient = HttpClientBuilder.create().build();
            final HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
            requestFactory.setConnectTimeout(1000);
            requestFactory.setReadTimeout(1000);
            final RestTemplate restTemplate = new RestTemplate(requestFactory);

            final Map<String, String> vars = new HashMap<>();
            vars.put("totalResults", "true");
            vars.put("pageSize", String.valueOf(pageSize));
            vars.put("resultsFrom", String.valueOf(resultsFrom));
            vars.put("companyRegistrationFrom", companyRegistrationFrom);

            final String response = restTemplate.getForObject(YTJ_API, String.class, vars);
            parseBusinessResponse(watch, response, pageSize, resultsFrom, companyRegistrationFrom, updateStatus);
        } catch (HttpClientErrorException e) {
            if (resultsFrom == 0 && e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                LOG.error("Business Id API responded with with status: " + e.getStatusCode() + " to initial request, marking result as failed, moving on.");
                updateManager.updateFailedStatus(updateStatus);
            } else {
                LOG.error("Business Id API responded with with status: " + e.getStatusCode() + ", retrying after 10 seconds.");
                retry = true;
            }
        } catch (Exception e) {
            LOG.error("Business Id API connect exception occurred with message: " + e.getMessage());
            retry = true;
        }

        if (retry) {
            if (retryCount <= MAX_RETRIES) {
                try {
                    Thread.sleep(10000);
                    fetchBusinessIds(pageSize, resultsFrom, companyRegistrationFrom, retryCount + 1, updateStatus);
                } catch (InterruptedException e1) {
                    LOG.error("Business ID retrying failed: " + e1.getMessage());
                }

            } else {
                LOG.error("Business Id API requesting is failing, moving on...");
                updateManager.updateFailedStatus(updateStatus);
            }
        }
    }

    private void parseBusinessResponse(final Stopwatch watch, final String response, final int pageSize, final int resultsFrom, final String companyRegistrationFrom, final UpdateStatus updateStatus) {

        final ObjectMapper mapper = new ObjectMapper();

        try {
            final JsonNode node = mapper.readTree(response);

            if (response != null && !response.isEmpty()) {
                final List<BusinessId> businessIds = businessIdParser.parseBusinessIdsFromJsonArray(DomainConstants.SOURCE_YTJ, response);
                LOG.info("BusinessId data loaded: " + businessIds.size() + " businessids found in " + watch);
                watch.reset().start();
                domain.persistBusinessIds(businessIds);
                LOG.info("BusinessId data persisted in " + watch);

                final String nextResultUri = node.path("nextResultsUri").asText();
                final Integer totalResults = node.path("totalResults").asInt();
                if (!businessIds.isEmpty() && businessIds.size() == YTJ_PAGESIZE && resultsFrom+pageSize < totalResults && nextResultUri != null && !nextResultUri.isEmpty()) {
                    fetchBusinessIds(pageSize, resultsFrom+pageSize, companyRegistrationFrom, updateStatus);
                } else {
                    updateManager.updateSuccessStatus(updateStatus);
                    LOG.info("Total business ID results loaded: " + totalResults);
                }
            }
        } catch (IOException e) {
            LOG.error("Business ID fetching has failed with response: " + response + ", message: " + e.getMessage());
        }
    }

    public void initializeOrRefresh() {
        // Initialize from the start.
        if (updateManager.shouldInitialize(DomainConstants.DATA_BUSINESSIDS)) {
            loadBusinessIds(HISTORY_START_FROM);

        // Load latest Business Id data starting from last successful update.
        } else {
            String nextVersion = updateManager.getNextUpdateVersion(DomainConstants.DATA_BUSINESSIDS);
            if (nextVersion != null) {
                nextVersion = Utils.yesterdayInIso8601();
            }
            if (updateManager.shouldUpdateData(DomainConstants.DATA_BUSINESSIDS, nextVersion)) {
                checkForNewData(nextVersion);
            }
        }
    }

    public boolean checkForNewData() {
        final String registrationFrom = Utils.yesterdayInIso8601();
        return checkForNewData(registrationFrom);
    }

    private boolean checkForNewData(final String registrationFrom) {
        boolean reIndex = false;
        LOG.info("Loading Business ID data from PRH using registrationFrom: " + registrationFrom);
        if (updateManager.shouldUpdateData(DomainConstants.DATA_BUSINESSIDS, registrationFrom)) {
            loadBusinessIds(registrationFrom);
            reIndex = true;
        } else {
            LOG.info("Business IDs already up to date, skipping...");
        }
        return reIndex;
    }

}
