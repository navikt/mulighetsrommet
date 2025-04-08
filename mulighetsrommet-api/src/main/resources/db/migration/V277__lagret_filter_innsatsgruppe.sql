update lagret_filter
set filter = jsonb_set(
        filter,
        '{innsatsgruppe,nokkel}',
        case filter -> 'innsatsgruppe' ->> 'nokkel'
            when 'STANDARD_INNSATS' then '"GODE_MULIGHETER"'
            when 'SITUASJONSBESTEMT_INNSATS' then '"TRENGER_VEILEDNING"'
            when 'SPESIELT_TILPASSET_INNSATS' then '"TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE"'
            when 'GRADERT_VARIG_TILPASSET_INNSATS' then '"JOBBE_DELVIS"'
            when 'VARIG_TILPASSET_INNSATS' then '"LITEN_MULIGHET_TIL_A_JOBBE"'
            end::jsonb)
where type = 'GJENNOMFORING_MODIA'
  and filter -> 'innsatsgruppe' ->> 'nokkel' in (
                                                 'STANDARD_INNSATS',
                                                 'SITUASJONSBESTEMT_INNSATS',
                                                 'SPESIELT_TILPASSET_INNSATS',
                                                 'GRADERT_VARIG_TILPASSET_INNSATS',
                                                 'VARIG_TILPASSET_INNSATS'
    );
