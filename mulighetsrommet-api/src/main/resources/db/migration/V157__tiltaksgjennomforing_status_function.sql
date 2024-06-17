create function tiltaksgjennomforing_status(start_dato date, slutt_dato date, avbrutt_tidspunkt timestamp)
returns varchar
language plpgsql
as
$$
begin
   return case
       when avbrutt_tidspunkt is not null and avbrutt_tidspunkt < start_dato then 'AVLYST'
       when avbrutt_tidspunkt is not null and avbrutt_tidspunkt >= start_dato then 'AVBRUTT'
       when slutt_dato is not null and date(now()) > slutt_dato then 'AVSLUTTET'
       when date(now()) >= start_dato then 'GJENNOMFORES'
       else 'PLANLAGT'
   end;
end;
$$;
