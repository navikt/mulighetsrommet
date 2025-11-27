with nav_enheter_for_nav_regioner as (select lagret_filter.id,
                                             jsonb_agg(
                                                     jsonb_build_object(
                                                             'navn', nav_enhet.navn,
                                                             'type', nav_enhet.type,
                                                             'enhetsnummer', nav_enhet.enhetsnummer,
                                                             'overordnetEnhet', nav_enhet.overordnet_enhet
                                                     )
                                             ) as nav_enheter
                                      from lagret_filter
                                               join nav_enhet on nav_enhet.overordnet_enhet = any
                                                                 (select jsonb_array_elements_text(lagret_filter.filter -> 'navRegioner')) and
                                                                 nav_enhet.type = 'LOKAL'
                                      where lagret_filter.type = 'AVTALE'
                                        and lagret_filter.filter -> 'navRegioner' is not null
                                      group by lagret_filter.id)
update lagret_filter
set filter = (filter - 'navRegioner' || jsonb_build_object('navEnheter', nav_enheter))
from nav_enheter_for_nav_regioner
where lagret_filter.id = nav_enheter_for_nav_regioner.id;
