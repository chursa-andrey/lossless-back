CREATE TABLE IF NOT EXISTS tracks (
  id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  genre_id           BIGINT      NOT NULL REFERENCES genres (id),
  created_by_user_id BIGINT      NOT NULL REFERENCES users (id),
  title              VARCHAR(255),
  artist_name        VARCHAR(255),
  album_title        VARCHAR(255),
  status             VARCHAR(32) NOT NULL,
  created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  updated_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_tracks_genre_id ON tracks (genre_id);
CREATE INDEX IF NOT EXISTS ix_tracks_created_by_user_id ON tracks (created_by_user_id);
CREATE INDEX IF NOT EXISTS ix_tracks_status_created_at ON tracks (status, created_at);
