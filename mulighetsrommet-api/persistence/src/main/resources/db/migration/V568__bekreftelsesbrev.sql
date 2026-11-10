drop view if exists view_tilsagn;

alter table tilsagn
    rename column journalpost_id to bekreftelsesbrev_journalpost_id;

alter table tilsagn
    rename column altinn_correspondence_id to bekreftelsesbrev_altinn_correspondence_id;
