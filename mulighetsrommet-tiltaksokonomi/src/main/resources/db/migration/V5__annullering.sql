alter table bestilling
    rename behandlet_av to opprettelse_behandlet_av;
alter table bestilling
    rename behandlet_tidspunkt to opprettelse_behandlet_tidspunkt;
alter table bestilling
    rename besluttet_av to opprettelse_besluttet_av;
alter table bestilling
    rename besluttet_tidspunkt to opprettelse_besluttet_tidspunkt;

alter table bestilling
    add annullering_behandlet_av        text,
    add annullering_behandlet_tidspunkt timestamptz,
    add annullering_besluttet_av        text,
    add annullering_besluttet_tidspunkt timestamptz;
