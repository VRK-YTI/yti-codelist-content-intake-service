package fi.vm.yti.codelist.intake.indexing;

public interface Indexing {

    boolean reIndexEverything();

    boolean reIndex(final String indexName, final String type);
}
