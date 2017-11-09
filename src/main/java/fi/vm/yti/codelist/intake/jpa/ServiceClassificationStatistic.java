package fi.vm.yti.codelist.intake.jpa;

import java.util.UUID;

public class ServiceClassificationStatistic {

    private UUID codeId;
    private Integer count;

    public ServiceClassificationStatistic() {
    }

    public ServiceClassificationStatistic(final UUID codeId,
                                          final Integer count) {
        this.codeId = codeId;
        this.count = count;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(final Integer count) {
        this.count = count;
    }
}
