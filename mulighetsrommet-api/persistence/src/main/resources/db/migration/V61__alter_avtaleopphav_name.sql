alter type avtaleopphav rename to opphav;

alter table tiltaksgjennomforing add column opphav opphav not null default 'ARENA';
