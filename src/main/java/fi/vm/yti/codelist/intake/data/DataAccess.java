package fi.vm.yti.codelist.intake.data;

/**
 * Generic Data Access interface.
 */
public interface DataAccess {

    void initializeOrRefresh();

    boolean checkForNewData();

}
