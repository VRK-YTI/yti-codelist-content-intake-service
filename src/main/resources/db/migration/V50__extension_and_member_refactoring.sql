-- Migration related work regarding extensionscheme and extension renaming.

ALTER TABLE extension RENAME TO member;
ALTER TABLE member RENAME extension_id TO broadermember_id;

ALTER TABLE member DROP CONSTRAINT fk_extension_id;
ALTER TABLE member ADD CONSTRAINT fk_broadermember_id FOREIGN KEY (broadermember_id) REFERENCES member(id);

ALTER TABLE member RENAME extensionvalue TO membervalue;
ALTER TABLE member RENAME extensionorder TO memberorder;

ALTER TABLE extension_preflabel RENAME TO member_preflabel;
ALTER TABLE member_preflabel RENAME extension_id TO member_id;

ALTER TABLE member_preflabel DROP CONSTRAINT extension_preflabel_pkey;
ALTER TABLE member_preflabel ADD CONSTRAINT member_preflabel_pkey PRIMARY KEY (member_id, language);

ALTER TABLE member_preflabel DROP CONSTRAINT fk_extension_id;
ALTER TABLE member_preflabel ADD CONSTRAINT fk_member_id FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE;

ALTER TABLE editedentity RENAME extension_id TO member_id;

-- ALTER TABLE editedentity DROP CONSTRAINT fk_extension_id;
-- ALTER TABLE editedentity ADD CONSTRAINT fk_member_id FOREIGN KEY (member_id) REFERENCES member(id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;