-- Sletter stale kvittering på oppgjort tilsagn
-- Vi har en bug hvor vi sender denne statusen til apiet, men det hadde egentlig ikke vært nødvendig
delete from kafka_consumer_record
where topic = 'team-mulighetsrommet.tiltaksokonomi.faktura-status-v1'
  and retries > 3
  and key in (convert_to('A-2024/14354-1-X', 'UTF8'), convert_to('A-2024/14339-1-X', 'UTF8'));
