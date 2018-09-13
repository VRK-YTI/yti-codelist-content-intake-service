-- Renaming coderegistry definition field to description

ALTER TABLE coderegistry_definition RENAME TO coderegistry_description;
ALTER TABLE coderegistry_description RENAME definition TO description;