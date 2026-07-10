alter table utbetaling_beregning_forhandsgodkjent drop constraint utbetaling_beregning_aft_utbetaling_id_fkey;
alter table utbetaling_beregning_fri drop constraint utbetaling_beregning_fri_utbetaling_id_fkey;
alter table utbetaling_stengt_hos_arrangor drop constraint utbetaling_stengt_hos_arrangor_utbetaling_id_fkey;
alter table utbetaling_deltakelse_manedsverk drop constraint utbetaling_deltakelse_manedsverk_utbetaling_id_fkey;
alter table utbetaling_deltakelse_periode drop constraint utbetaling_deltakelse_periode_utbetaling_id_fkey;

alter table utbetaling_beregning_forhandsgodkjent add constraint utbetaling_beregning_forhandsgodkjent_utbetaling_id_fkey
    foreign key (utbetaling_id) references utbetaling(id) on delete cascade;
alter table utbetaling_beregning_fri add constraint utbetaling_beregning_fri_utbetaling_id_fkey
    foreign key (utbetaling_id) references utbetaling(id) on delete cascade;
alter table utbetaling_stengt_hos_arrangor add constraint utbetaling_stengt_hos_arrangor_utbetaling_id_fkey
    foreign key (utbetaling_id) references utbetaling(id) on delete cascade;
alter table utbetaling_deltakelse_manedsverk add constraint utbetaling_deltakelse_manedsverk_utbetaling_id_fkey
    foreign key (utbetaling_id) references utbetaling(id) on delete cascade;
alter table utbetaling_deltakelse_periode add constraint utbetaling_deltakelse_periode_utbetaling_id_fkey
    foreign key (utbetaling_id) references utbetaling(id) on delete cascade;
