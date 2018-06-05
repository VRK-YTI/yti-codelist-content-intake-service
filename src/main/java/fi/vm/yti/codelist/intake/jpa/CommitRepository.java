package fi.vm.yti.codelist.intake.jpa;

import java.util.Date;
import java.util.UUID;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fi.vm.yti.codelist.intake.model.Commit;

@Repository
@Transactional
public interface CommitRepository extends CrudRepository<Commit, String> {

    Commit findByTraceId(@Param("traceId") final String traceId);

    @Query(value = "SELECT c.modified FROM commit AS c WHERE c.id IN (SELECT e.commit_id FROM editedentity AS e WHERE e.coderegistry_id = :codeRegistryId) ORDER BY c.modified DESC LIMIT 1", nativeQuery = true)
    Date findLatestModifiedByCodeRegistryId(@Param("codeRegistryId") final UUID codeRegistryId);

    @Query(value = "SELECT c.modified FROM commit AS c WHERE c.id IN (SELECT e.commit_id FROM editedentity AS e WHERE e.codescheme_id = :codeSchemeId) ORDER BY c.modified DESC LIMIT 1", nativeQuery = true)
    Date findLatestModifiedByCodeSchemeId(@Param("codeSchemeId") final UUID codeSchemeId);

    @Query(value = "SELECT c.modified FROM commit AS c WHERE c.id IN (SELECT e.commit_id FROM editedentity AS e WHERE e.code_id = :codeId) ORDER BY c.modified DESC LIMIT 1", nativeQuery = true)
    Date findLatestModifiedByCodeId(@Param("codeId") final UUID codeId);

    @Query(value = "SELECT c.modified FROM commit AS c WHERE c.id IN (SELECT e.commit_id FROM editedentity AS e WHERE e.extensionscheme_id = :extensionSchemeId) ORDER BY c.modified DESC LIMIT 1", nativeQuery = true)
    Date findLatestModifiedByExtensionSchemeId(@Param("extensionSchemeId") final UUID extensionSchemeId);

    @Query(value = "SELECT c.modified FROM commit AS c WHERE c.id IN (SELECT e.commit_id FROM editedentity AS e WHERE e.extension_id = :extensionId) ORDER BY c.modified DESC LIMIT 1", nativeQuery = true)
    Date findLatestModifiedByExtensionId(@Param("extensionId") final UUID extensionId);

    @Query(value = "SELECT c.modified FROM commit AS c WHERE c.id IN (SELECT e.commit_id FROM editedentity AS e WHERE e.propertytype_id = :propertyTypeId) ORDER BY c.modified DESC LIMIT 1", nativeQuery = true)
    Date findLatestModifiedByPropertyTypeId(@Param("propertyTypeId") final UUID propertyTypeId);

    @Query(value = "SELECT c.modified FROM commit AS c WHERE c.id IN (SELECT e.commit_id FROM editedentity AS e WHERE e.externalreference_id = :externalReferenceId) ORDER BY c.modified DESC LIMIT 1", nativeQuery = true)
    Date findLatestModifiedByExternalReferenceId(@Param("externalReferenceId") final UUID externalReferenceId);

    @Query(value = "SELECT c.modified FROM commit AS c WHERE c.id IN (SELECT e.commit_id FROM editedentity AS e WHERE e.coderegistry_id = :codeRegistryId) ORDER BY c.modified ASC LIMIT 1", nativeQuery = true)
    Date findCreatedByCodeRegistryId(@Param("codeRegistryId") final UUID codeRegistryId);

    @Query(value = "SELECT c.modified FROM commit AS c WHERE c.id IN (SELECT e.commit_id FROM editedentity AS e WHERE e.codescheme_id = :codeSchemeId) ORDER BY c.modified ASC LIMIT 1", nativeQuery = true)
    Date findCreatedByCodeSchemeId(@Param("codeSchemeId") final UUID codeSchemeId);

    @Query(value = "SELECT c.modified FROM commit AS c WHERE c.id IN (SELECT e.commit_id FROM editedentity AS e WHERE e.code_id = :codeId) ORDER BY c.modified ASC LIMIT 1", nativeQuery = true)
    Date findCreatedByCodeId(@Param("codeId") final UUID codeId);

    @Query(value = "SELECT c.modified FROM commit AS c WHERE c.id IN (SELECT e.commit_id FROM editedentity AS e WHERE e.extensionscheme_id = :extensionSchemeId) ORDER BY c.modified ASC LIMIT 1", nativeQuery = true)
    Date findCreatedByExtensionSchemeId(@Param("extensionSchemeId") final UUID extensionSchemeId);

    @Query(value = "SELECT c.modified FROM commit AS c WHERE c.id IN (SELECT e.commit_id FROM editedentity AS e WHERE e.extension_id = :extensionId) ORDER BY c.modified ASC LIMIT 1", nativeQuery = true)
    Date findCreatedByExtensionId(@Param("extensionId") final UUID extensionId);

    @Query(value = "SELECT c.modified FROM commit AS c WHERE c.id IN (SELECT e.commit_id FROM editedentity AS e WHERE e.propertytype_id = :propertyTypeId) ORDER BY c.modified ASC LIMIT 1", nativeQuery = true)
    Date findCreatedByPropertyTypeId(@Param("propertyTypeId") final UUID propertyTypeId);

    @Query(value = "SELECT c.modified FROM commit AS c WHERE c.id IN (SELECT e.commit_id FROM editedentity AS e WHERE e.externalreference_id = :externalReferenceId) ORDER BY c.modified ASC LIMIT 1", nativeQuery = true)
    Date findCreatedByExternalReferenceId(@Param("externalReferenceId") final UUID externalReferenceId);
}
