alter table tilsagn
    add column beregning_prismodell text;

update tilsagn t
set beregning_prismodell = case t.beregning_type
                               when 'ANNEN_AVTALT_PRIS' then 'ANNEN_AVTALT_PRIS'
                               when 'FRI' then 'ANSKAFFET_ENKELTPLASS'
                               when 'PRIS_PER_MANEDSVERK' then 'AVTALT_PRIS_PER_BENYTTET_PLASS_PER_MANED'
                               when 'PRIS_PER_UKESVERK' then 'AVTALT_PRIS_PER_BENYTTET_PLASS_PER_UKE'
                               when 'PRIS_PER_HELE_UKESVERK' then 'AVTALT_PRIS_PER_BENYTTET_PLASS_PER_HELE_UKE'
                               when 'PRIS_PER_TIME_OPPFOLGING' then 'AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER'
                               when 'FAST_SATS_PER_TILTAKSPLASS_PER_MANED' then case tiltakstype.tiltakskode
                                   when 'TILRETTELAGT_ARBEID_ORDINAER' then 'FAST_SATS_PER_AVTALT_PLASS_PER_MANED'
                                   else 'FAST_SATS_PER_BENYTTET_PLASS_PER_MANED'
                               end
    end
from gjennomforing
         join tiltakstype on tiltakstype.id = gjennomforing.tiltakstype_id
where gjennomforing.id = t.gjennomforing_id;

alter table tilsagn
    alter column beregning_prismodell set not null,
    add foreign key (beregning_prismodell) references prismodell_type (value) on update cascade;
