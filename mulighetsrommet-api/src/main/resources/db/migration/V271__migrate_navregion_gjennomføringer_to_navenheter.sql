INSERT INTO gjennomforing_nav_enhet(gjennomforing_id, enhetsnummer)
SELECT id,nav_region FROM gjennomforing WHERE nav_region IS NOT NULL
ON CONFLICT (gjennomforing_id, enhetsnummer) DO NOTHING;
