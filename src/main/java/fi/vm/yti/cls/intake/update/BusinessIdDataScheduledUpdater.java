package fi.vm.yti.cls.intake.update;

import fi.vm.yti.cls.intake.data.YtjDataAccess;
import fi.vm.yti.cls.intake.domain.Domain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static fi.vm.yti.cls.intake.domain.DomainConstants.ELASTIC_INDEX_CUSTOMCODES;
import static fi.vm.yti.cls.intake.domain.DomainConstants.ELASTIC_TYPE_BUSINESSID;


/**
 * Class that handles triggering of scheduled for YTJ / PRH Business ID data fetching and processing.
 */
@Component
public class BusinessIdDataScheduledUpdater implements DataUpdate {

    private static final Logger LOG = LoggerFactory.getLogger(BusinessIdDataScheduledUpdater.class);

    private YtjDataAccess m_ytjDataAccess;

    private Domain m_domain;


    @Inject
    public BusinessIdDataScheduledUpdater(final YtjDataAccess ytjDataAccess,
                                          final Domain domain) {

        m_ytjDataAccess = ytjDataAccess;

        m_domain = domain;

    }


    /**
     * Fetches yesterdays new business id registrations on a daily basis at 02:00 am.
     *
     * If new data is found, new index to ElasticSearch is made.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void updateData() {

        LOG.info("Scheduled business ID update starting...");

        final boolean reIndex = m_ytjDataAccess.checkForNewData();

        if (reIndex) {
            m_domain.deleteTypeFromIndex(ELASTIC_INDEX_CUSTOMCODES, ELASTIC_TYPE_BUSINESSID);
            m_domain.ensureNestedPrefLabelsMapping(ELASTIC_INDEX_CUSTOMCODES, ELASTIC_TYPE_BUSINESSID);
            m_domain.indexBusinessIds();
        }

    }

}
