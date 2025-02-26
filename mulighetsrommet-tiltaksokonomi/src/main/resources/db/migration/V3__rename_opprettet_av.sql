alter table bestilling rename column opprettet_tidspunkt to behandlet_tidspunkt;
alter table bestilling rename column opprettet_av to behandlet_av;

alter table faktura rename column opprettet_av to behandlet_av;
alter table faktura rename column opprettet_tidspunkt to behandlet_tidspunkt;
