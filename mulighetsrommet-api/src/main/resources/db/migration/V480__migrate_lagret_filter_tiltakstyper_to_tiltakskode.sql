update lagret_filter
set filter = jsonb_set(
    filter,
    '{tiltakstyper}',
    coalesce(
        (select jsonb_agg(to_jsonb(t.tiltakskode::text))
         from jsonb_array_elements_text(filter -> 'tiltakstyper') as item_id
                  join tiltakstype t on t.id = item_id::uuid),
        '[]'::jsonb
    )
)
where type in ('GJENNOMFORING', 'AVTALE', 'INNSENDING')
  and filter ? 'tiltakstyper'
  and jsonb_array_length(filter -> 'tiltakstyper') > 0;
