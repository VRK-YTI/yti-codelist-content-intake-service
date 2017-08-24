package fi.vm.yti.cls.intake.data;


/**
 * Generic Data Access interface.
 */
public interface DataAccess {

    void initializeOrRefresh();

    boolean checkForNewData();

}
