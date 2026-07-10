with korreksjon as (select *
                    from utbetaling
                    where korreksjon_begrunnelse is not null
                      and korreksjon_gjelder_utbetaling_id is null),
     korreksjon_med_utbetaling as (select k.id as k_id, u.id as u_id, u.periode
                                   from utbetaling u
                                            join korreksjon k
                                                 on k.id != u.id and
                                                    k.gjennomforing_id = u.gjennomforing_id and
                                                    k.periode = u.periode
                                   where u.korreksjon_begrunnelse is null),
     korreksjon_med_unik_utbetaling as (select k_id
                                        from korreksjon_med_utbetaling
                                        group by k_id
                                        having count(*) = 1),
     korreksjon_til_oppdatering as (select *
                                    from korreksjon_med_utbetaling
                                    where k_id in (select * from korreksjon_med_unik_utbetaling))
update utbetaling
set korreksjon_gjelder_utbetaling_id = u_id
from korreksjon_til_oppdatering
where id = k_id;
