package fi.vm.yti.codelist.intake.jpa;

public class ServiceClassificationStatistic {

    private Integer count;

    public ServiceClassificationStatistic() {
    }

    public ServiceClassificationStatistic(final Integer count) {
        this.count = count;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(final Integer count) {
        this.count = count;
    }
}
