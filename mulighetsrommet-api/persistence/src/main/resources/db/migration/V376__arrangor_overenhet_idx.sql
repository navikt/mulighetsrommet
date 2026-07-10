DROP INDEX IF EXISTS arrangor_overordnet_enhet_idx;

CREATE INDEX arrangor_overordnet_enhet_idx
  ON arrangor (overordnet_enhet);
