ALTER TABLE document_versions ADD COLUMN source_file BYTEA;
ALTER TABLE document_versions ADD COLUMN source_filename VARCHAR(255);
ALTER TABLE document_versions ADD COLUMN source_content_type VARCHAR(120);