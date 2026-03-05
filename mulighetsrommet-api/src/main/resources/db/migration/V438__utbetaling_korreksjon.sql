drop view if exists view_utbetaling;

update utbetaling
set beskrivelse = null
where beskrivelse = '';

alter table utbetaling
    add korreksjon_gjelder_utbetaling_id uuid references utbetaling (id);

alter table utbetaling
    rename beskrivelse to korreksjon_begrunnelse;
