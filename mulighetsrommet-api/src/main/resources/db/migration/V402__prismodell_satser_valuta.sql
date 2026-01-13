-- Legg til valuta "NOK" i alle eksisterende prismodell satser
UPDATE avtale_prismodell SET satser = ( SELECT jsonb_agg(jsonb_set(obj, '{valuta}', '"NOK"')) FROM jsonb_array_elements(satser) AS obj ) WHERE satser IS NOT NULL;
