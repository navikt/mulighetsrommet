drop view if exists view_gjennomforing;
drop view if exists view_datavarehus_tiltak;

alter table gjennomforing
    add column artskontokode text;

update gjennomforing
set artskontokode = case
                        when gjennomforing.oppstart = 'ENKELTPLASS' then 'ENKELTPLASS_' || tiltakstype.tiltakskode
                        else tiltakstype.tiltakskode
    end from tiltakstype
where gjennomforing.tiltakstype_id = tiltakstype.id
  and tiltakstype.tiltakskode in (
    'ARBEIDSMARKEDSOPPLAERING'
    , 'FAG_OG_YRKESOPPLAERING'
    , 'NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV'
    , 'STUDIESPESIALISERING');
