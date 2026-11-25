ALTER TABLE file_versions ADD COLUMN multipart_upload_id VARCHAR(512);

ALTER TABLE file_versions ADD COLUMN upload_url_expires_at TIMESTAMPTZ NOT NULL;