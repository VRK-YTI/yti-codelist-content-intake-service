package fi.vm.yti.codelist.intake.jpa;

public class DataClassificationStatistic {

    private Integer count;

    public DataClassificationStatistic() {
    }

    public DataClassificationStatistic(final Integer count) {
        this.count = count;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(final Integer count) {
        this.count = count;
    }
}
