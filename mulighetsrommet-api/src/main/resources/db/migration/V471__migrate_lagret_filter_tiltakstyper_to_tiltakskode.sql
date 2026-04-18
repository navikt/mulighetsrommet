-- Replace sanityId with tiltakskode in the tiltakstyper array
update lagret_filter
set filter = jsonb_set(
        filter,
        '{tiltakstyper}',
        coalesce(
                (select jsonb_agg(
                                jsonb_set(item, '{id}', to_jsonb(t.tiltakskode::text))
                        )
                 from jsonb_array_elements(filter -> 'tiltakstyper') as item
                          join tiltakstype t on t.sanity_id = (item ->> 'id')::uuid),
                '[]'::jsonb
        )
             )
where type = 'GJENNOMFORING_MODIA'
  and jsonb_array_length(filter -> 'tiltakstyper') > 0;
