package fi.vm.yti.codelist.intake.indexing;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.springframework.scheduling.annotation.Scheduled;

@Singleton
public class ScheduledIndexer {

    private final Indexing indexing;

    @Inject
    public ScheduledIndexer(final Indexing indexing) {
        this.indexing = indexing;
    }

    @Scheduled(cron = "0 */1 * * * *")
    public void updateIndex() {
        indexing.reIndexEverythingIfNecessary();
    }
}
