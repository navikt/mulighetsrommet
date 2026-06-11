alter table tiltaksgjennomforing
    rename column avbrutt_tidspunkt to avsluttet_tidspunkt;

alter table tiltaksgjennomforing
    drop constraint ck_avbrutttidspunktaarsak;

update tiltaksgjennomforing
set avsluttet_tidspunkt = slutt_dato + interval '1' day
where avsluttet_tidspunkt is null
  and (slutt_dato + interval '1' day) < now();

create or replace function tiltaksgjennomforing_status(
    start_dato date,
    slutt_dato date,
    avbrutt_tidspunkt timestamp
)
    returns varchar
    language plpgsql
as
$$
begin
    return case
               when avbrutt_tidspunkt is null then 'GJENNOMFORES'
               when avbrutt_tidspunkt < start_dato then 'AVLYST'
               when slutt_dato is null or avbrutt_tidspunkt < slutt_dato + interval '1' day then 'AVBRUTT'
               else 'AVSLUTTET'
        end;
end;
$$;
