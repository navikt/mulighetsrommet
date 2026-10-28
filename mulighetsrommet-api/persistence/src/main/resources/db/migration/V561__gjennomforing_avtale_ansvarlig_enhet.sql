drop view if exists view_gjennomforing;
drop view if exists view_gjennomforing_kompakt;

update gjennomforing g
set ansvarlig_enhet = coalesce(
        (select t.kostnadssted from tilsagn t where t.gjennomforing_id = g.id and t.tilsagn_type = 'TILSAGN' order by created_at desc limit 1),
        g.arena_ansvarlig_enhet,
        (select gne.enhetsnummer from gjennomforing_nav_enhet gne join nav_enhet e on e.enhetsnummer = gne.enhetsnummer where gne.gjennomforing_id = g.id and e.type <> 'FYLKE' order by gne.enhetsnummer limit 1)
    )
where g.gjennomforing_type = 'AVTALE'
  and g.ansvarlig_enhet is null;

do
$$
    declare
        antall_uten_enhet int;
    begin
        select count(*)
        into antall_uten_enhet
        from gjennomforing
        where gjennomforing_type = 'AVTALE'
          and ansvarlig_enhet is null;

        if antall_uten_enhet > 0 then
            raise exception 'Etterfylling feilet: % gjennomføringer av type AVTALE mangler ansvarlig_enhet',
                antall_uten_enhet;
        end if;
    end
$$;
