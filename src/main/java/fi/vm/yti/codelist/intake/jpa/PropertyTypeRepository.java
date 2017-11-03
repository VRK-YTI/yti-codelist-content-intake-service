package fi.vm.yti.codelist.intake.jpa;

import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import fi.vm.yti.codelist.common.model.PropertyType;

@Repository
public interface PropertyTypeRepository extends CrudRepository<PropertyType, String> {

    PropertyType findById(final String id);

    Set<PropertyType> findAll();
}
