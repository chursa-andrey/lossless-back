CREATE TABLE IF NOT EXISTS track_audio_files (
  id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  track_id          BIGINT       NOT NULL REFERENCES tracks (id) ON DELETE CASCADE,
  storage_provider  VARCHAR(32)  NOT NULL,
  storage_key       VARCHAR(512) NOT NULL,
  original_filename VARCHAR(255),
  title             VARCHAR(255),
  artist_name       VARCHAR(255),
  album_title       VARCHAR(255),
  extension         VARCHAR(16)  NOT NULL,
  embedded_genre    VARCHAR(120),
  size_bytes        BIGINT       NOT NULL,
  checksum_sha256   CHAR(64)     NOT NULL,
  duration_seconds  INTEGER,
  sample_rate_hz    INTEGER,
  bit_depth         INTEGER,
  channels          INTEGER,
  bitrate_kbps      INTEGER,
  created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  CONSTRAINT ck_track_audio_files_size_positive CHECK (size_bytes > 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_track_audio_files_track_id ON track_audio_files (track_id);
CREATE UNIQUE INDEX IF NOT EXISTS ux_track_audio_files_storage_key ON track_audio_files (storage_key);
