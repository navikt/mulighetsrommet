INSERT INTO gjennomforing_nav_enhet(gjennomforing_id, enhetsnummer, created_at, updated_at)
SELECT id,nav_region, created_at, updated_at FROM gjennomforing WHERE nav_region IS NOT NULL
