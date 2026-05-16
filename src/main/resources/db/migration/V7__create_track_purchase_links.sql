CREATE TABLE IF NOT EXISTS track_purchase_links (
  id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  track_id   BIGINT        NOT NULL REFERENCES tracks (id) ON DELETE CASCADE,
  url        VARCHAR(2048) NOT NULL,
  position   INTEGER       NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_track_purchase_links_track_id ON track_purchase_links (track_id);
