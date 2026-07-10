alter table tiltakstype
    alter column navn set data type text collate "nb-NO-x-icu";

alter table avtale
    alter column navn set data type text collate "nb-NO-x-icu";

create index avtale_navn_idx
    on avtale(navn);

create index avtale_avslutningsstatus_idx
    on avtale(avslutningsstatus);

create index tiltakstype_navn_idx
    on tiltakstype(navn);
