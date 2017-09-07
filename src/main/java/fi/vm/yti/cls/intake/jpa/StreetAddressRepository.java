package fi.vm.yti.cls.intake.jpa;

import fi.vm.yti.cls.common.model.StreetAddress;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface StreetAddressRepository extends PagingAndSortingRepository<StreetAddress, String> {

    StreetAddress findById(final String id);

    @EntityGraph(value = "streetAddressListing", type = EntityGraph.EntityGraphType.FETCH)
    Set<StreetAddress> findAll();

    @Query(value = "SELECT DISTINCT sa.id FROM StreetAddress sa")
    Set<String> findAllIds();

    @EntityGraph(value = "streetAddressListing", type = EntityGraph.EntityGraphType.LOAD)
    Set<StreetAddress> findByIdIn(final Iterable<String> ids);

}