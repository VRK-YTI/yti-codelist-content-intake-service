package fi.vm.yti.cls.intake.update;

import fi.vm.yti.cls.intake.data.PostiDataAccess;
import fi.vm.yti.cls.intake.domain.Domain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.inject.Inject;


/**
 * Class that handles triggering of scheduled for Posti Data fetching and processing.
 */
@Component
public class PostiDataScheduledUpdater implements DataUpdate {

    private static final Logger LOG = LoggerFactory.getLogger(PostiDataScheduledUpdater.class);

    private PostiDataAccess m_postiDataAccess;

    private Domain m_domain;


    @Inject
    public PostiDataScheduledUpdater(final PostiDataAccess postiDataAccess,
                                     final Domain domain) {

        m_postiDataAccess = postiDataAccess;

        m_domain = domain;

    }


    /**
     * Checks for new Posti data file on a daily basis at 02:00 am.
     *
     * If a new file is found, it is downloaded, parsed, persisted and reindexed to ElasticSearch.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void updateData() {

        LOG.info("Scheduled posti data update starting...");

        final boolean reIndex = m_postiDataAccess.checkForNewData();

        if (reIndex) {
            m_domain.reIndexEverything();
        }

    }

}
