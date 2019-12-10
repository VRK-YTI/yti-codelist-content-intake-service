package fi.vm.yti.codelist.intake.jpa;

import java.util.Set;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import fi.vm.yti.codelist.intake.model.PropertyType;

@Repository
@Transactional
public interface PropertyTypeRepository extends CrudRepository<PropertyType, String> {

    PropertyType findById(final UUID id);

    PropertyType findByLocalName(final String localName);

    PropertyType findByContextAndLocalName(final String context,
                                           final String localName);

    Set<PropertyType> findAll();
}
