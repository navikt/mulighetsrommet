update kafka_consumer_record
set value = convert_to(
        jsonb_set(
                jsonb_set(
                        convert_from(value, 'UTF8')::jsonb,
                        '{payload,valuta}',
                        '"NOK"'),
                '{payload,betalingsinformasjon,type}',
                '"no.nav.tiltak.okonomi.OpprettFaktura.Betalingsinformasjon.BBan"')::text,
        'UTF8')
where (convert_from(value, 'UTF8')::jsonb ->> 'type') = 'FAKTURA'
  and (convert_from(value, 'UTF8')::jsonb -> 'payload' ->> 'valuta') is null
  and (convert_from(value, 'UTF8')::jsonb -> 'payload' -> 'betalingsinformasjon' ->> 'type') is null;
