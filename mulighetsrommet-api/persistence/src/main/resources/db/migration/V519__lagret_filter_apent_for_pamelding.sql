update lagret_filter
set filter = jsonb_set(
        filter,
        '{apentForPamelding}',
        case filter ->> 'apentForPamelding'
            when 'APENT' then '["APENT"]'
            when 'STENGT' then '["STENGT"]'
            when 'APENT_ELLER_STENGT' then 'null'
            end::jsonb)
where type = 'GJENNOMFORING_MODIA'
  and filter ->> 'apentForPamelding' in (
                                         'APENT',
                                         'STENGT',
                                         'APENT_ELLER_STENGT'
);
