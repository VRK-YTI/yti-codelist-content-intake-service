package fi.vm.yti.codelist.intake.jpa;

import java.util.Set;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import fi.vm.yti.codelist.intake.model.Organization;

@Repository
@Transactional
public interface OrganizationRepository extends CrudRepository<Organization, String> {

    Organization findById(final UUID id);

    Set<Organization> findByRemovedIsFalse();

    Set<Organization> findByRemovedIsFalseAndCodeSchemesIsNotNullAndParentIsNull();

    Set<Organization> findByParentId(UUID parentId);

    Set<Organization> findAll();
}
