with skal_bli_oppgjort as (select t.id, ul.gjor_opp_tilsagn, u.utbetales_tidligst_tidspunkt
                           from tilsagn t
                                    join utbetaling_linje ul on t.id = ul.tilsagn_id
                                    join utbetaling u on ul.utbetaling_id = u.id
                           where t.status = 'OPPGJORT'
                             and t.bestilling_status = 'AKTIV'
                             and ul.gjor_opp_tilsagn),
     oppgjort_men_aktiv as (select t.id
                            from tilsagn t
                            where t.status = 'OPPGJORT'
                              and t.bestilling_status = 'AKTIV'),
     ikke_oppgjort as (select id
                       from oppgjort_men_aktiv
                       where oppgjort_men_aktiv.id not in (select id from skal_bli_oppgjort)),
     bestilling_til_oppgjor as (select t.bestillingsnummer,
                                       oppgjor.behandlet_av,
                                       oppgjor.behandlet_tidspunkt,
                                       oppgjor.besluttet_av,
                                       oppgjor.besluttet_tidspunkt
                                from tilsagn t
                                         join gjennomforing g on g.id = t.gjennomforing_id
                                         join tiltakstype tt on tt.id = g.tiltakstype_id
                                         join totrinnskontroll oppgjor
                                              on oppgjor.entity_id = t.id
                                                  and oppgjor.type = 'TILSAGN_OPPGJOR'
                                where t.id in (select id from ikke_oppgjort))
insert
into kafka_producer_record (topic, key, value, headers_json)
select 'team-mulighetsrommet.tiltaksokonomi.bestillinger-v1' as topic,
       convert_to(bestillingsnummer, 'UTF8')                 as key,
       convert_to(
               jsonb_build_object(
                       'type',
                       'GJOR_OPP_BESTILLING',
                       'payload',
                       jsonb_build_object(
                               'bestillingsnummer', bestillingsnummer,
                               'behandletAv', jsonb_build_object('type', 'NAV_ANSATT', 'navIdent', behandlet_av),
                               'behandletTidspunkt', behandlet_tidspunkt,
                               'besluttetAv', jsonb_build_object('type', 'NAV_ANSATT', 'navIdent', besluttet_av),
                               'besluttetTidspunkt', besluttet_tidspunkt
                       )
               )::text,
               'UTF8'
       )                                                     as value,
       '[]'::jsonb                                           as headers_json
from bestilling_til_oppgjor;
