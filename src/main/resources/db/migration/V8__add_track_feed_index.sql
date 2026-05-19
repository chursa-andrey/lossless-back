CREATE INDEX IF NOT EXISTS ix_tracks_feed ON tracks (status, created_at DESC, id DESC);
