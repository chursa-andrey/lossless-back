CREATE TABLE IF NOT EXISTS genres (
  id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  name       VARCHAR(100) NOT NULL,
  slug       VARCHAR(80)  NOT NULL,
  active     BOOLEAN      NOT NULL DEFAULT true,
  sort_order INTEGER      NOT NULL DEFAULT 0,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_genres_slug ON genres (slug);
CREATE INDEX IF NOT EXISTS ix_genres_active_sort_order ON genres (active, sort_order);

INSERT INTO genres (name, slug, active, sort_order)
SELECT 'Rock', 'rock', true, 10
WHERE NOT EXISTS (SELECT 1 FROM genres WHERE slug = 'rock');

INSERT INTO genres (name, slug, active, sort_order)
SELECT 'Pop', 'pop', true, 20
WHERE NOT EXISTS (SELECT 1 FROM genres WHERE slug = 'pop');

INSERT INTO genres (name, slug, active, sort_order)
SELECT 'Hip-Hop / Rap', 'hipHopRap', true, 30
WHERE NOT EXISTS (SELECT 1 FROM genres WHERE slug = 'hipHopRap');

INSERT INTO genres (name, slug, active, sort_order)
SELECT 'Electronic', 'electronic', true, 40
WHERE NOT EXISTS (SELECT 1 FROM genres WHERE slug = 'electronic');

INSERT INTO genres (name, slug, active, sort_order)
SELECT 'Jazz', 'jazz', true, 50
WHERE NOT EXISTS (SELECT 1 FROM genres WHERE slug = 'jazz');

INSERT INTO genres (name, slug, active, sort_order)
SELECT 'Classical', 'classical', true, 60
WHERE NOT EXISTS (SELECT 1 FROM genres WHERE slug = 'classical');

INSERT INTO genres (name, slug, active, sort_order)
SELECT 'R&B / Soul', 'rnbSoul', true, 70
WHERE NOT EXISTS (SELECT 1 FROM genres WHERE slug = 'rnbSoul');

INSERT INTO genres (name, slug, active, sort_order)
SELECT 'Metal', 'metal', true, 80
WHERE NOT EXISTS (SELECT 1 FROM genres WHERE slug = 'metal');

INSERT INTO genres (name, slug, active, sort_order)
SELECT 'Indie', 'indie', true, 90
WHERE NOT EXISTS (SELECT 1 FROM genres WHERE slug = 'indie');

INSERT INTO genres (name, slug, active, sort_order)
SELECT 'Reggae', 'reggae', true, 100
WHERE NOT EXISTS (SELECT 1 FROM genres WHERE slug = 'reggae');
