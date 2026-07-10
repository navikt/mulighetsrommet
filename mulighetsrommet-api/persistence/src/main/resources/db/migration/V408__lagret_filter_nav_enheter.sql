update lagret_filter
set filter = jsonb_set(
        filter,
        '{navEnheter}',
        (select jsonb_agg(elem -> 'enhetsnummer')
         from jsonb_array_elements(filter -> 'navEnheter') as elem))
where filter ? 'navEnheter'
  and jsonb_array_length(filter -> 'navEnheter') > 0;
