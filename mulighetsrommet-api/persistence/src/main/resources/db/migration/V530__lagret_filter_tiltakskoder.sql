update lagret_filter
set filter = jsonb_set(
    filter,
    '{tiltakstyper}',
    (
        select coalesce(
            jsonb_agg(
                case
                    when jsonb_typeof(item) = 'object' then
                        case item ->> 'id'
                            when 'MIDLERTIDIG_LONNSTLSKUDD' then jsonb_set(item, '{id}', to_jsonb('MIDLERTIDIG_LONNSTILSKUDD'::text))
                            when 'VARIG_LONNSTILSKUD' then jsonb_set(item, '{id}', to_jsonb('VARIG_LONNSTILSKUDD'::text))
                            when 'INKLUDERINGSTILSKUD' then jsonb_set(item, '{id}', to_jsonb('INKLUDERINGSTILSKUDD'::text))
                            when 'FIREARIG_LONNSTILSUDD' then jsonb_set(item, '{id}', to_jsonb('FIREARIG_LONNSTILSKUDD'::text))
                            else item
                        end
                    when jsonb_typeof(item) = 'string' then
                        case item #>> '{}'
                            when 'MIDLERTIDIG_LONNSTLSKUDD' then to_jsonb('MIDLERTIDIG_LONNSTILSKUDD'::text)
                            when 'VARIG_LONNSTILSKUD' then to_jsonb('VARIG_LONNSTILSKUDD'::text)
                            when 'INKLUDERINGSTILSKUD' then to_jsonb('INKLUDERINGSTILSKUDD'::text)
                            when 'FIREARIG_LONNSTILSUDD' then to_jsonb('FIREARIG_LONNSTILSKUDD'::text)
                            else item
                        end
                    else item
                end
                order by ord
            ),
            '[]'::jsonb
        )
        from jsonb_array_elements(filter -> 'tiltakstyper') with ordinality as t(item, ord)
    )
)
where filter ? 'tiltakstyper'
  and jsonb_typeof(filter -> 'tiltakstyper') = 'array'
  and exists(
    select 1
    from jsonb_array_elements(filter -> 'tiltakstyper') as t(item)
    where item ->> 'id' in (
        'MIDLERTIDIG_LONNSTLSKUDD',
        'VARIG_LONNSTILSKUD',
        'INKLUDERINGSTILSKUD',
        'FIREARIG_LONNSTILSUDD'
    )
       or item #>> '{}' in (
        'MIDLERTIDIG_LONNSTLSKUDD',
        'VARIG_LONNSTILSKUD',
        'INKLUDERINGSTILSKUD',
        'FIREARIG_LONNSTILSUDD'
    )
  );
