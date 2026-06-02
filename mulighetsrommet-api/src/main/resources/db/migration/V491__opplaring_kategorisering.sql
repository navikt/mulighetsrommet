--- forerkort
create table avtale_amo_kategorisering_forerkort
(
    avtale_id uuid references avtale_amo_kategorisering (avtale_id) on delete cascade,
    forerkort_id uuid references opplaring_kategorisering_forerkort (id) on delete cascade,
    primary key (avtale_id, forerkort_id)
);

insert into avtale_amo_kategorisering_forerkort (avtale_id, forerkort_id)
select distinct aak.avtale_id, okf.id
from avtale_amo_kategorisering aak
         cross join unnest(aak.forerkort) as elem
         inner join opplaring_kategorisering_forerkort okf on okf.kode = elem::text
where aak.forerkort is not null and array_length(aak.forerkort, 1) > 0
on conflict do nothing;


create table gjennomforing_amo_kategorisering_forerkort
(
    gjennomforing_id uuid references gjennomforing_amo_kategorisering (gjennomforing_id) on delete cascade,
    forerkort_id uuid references opplaring_kategorisering_forerkort (id) on delete cascade,
    primary key (gjennomforing_id, forerkort_id)
);

insert into gjennomforing_amo_kategorisering_forerkort (gjennomforing_id, forerkort_id)
select distinct gak.gjennomforing_id, okf.id
from gjennomforing_amo_kategorisering gak
         cross join unnest(gak.forerkort) as elem
         inner join opplaring_kategorisering_forerkort okf on okf.kode = elem::text
where gak.forerkort is not null and array_length(gak.forerkort, 1) > 0
on conflict do nothing;

-- avtale bransje og kurstype
alter table avtale_amo_kategorisering
    add column bransje_id  uuid,
    add column kurstype_id uuid;

update avtale_amo_kategorisering a
set bransje_id = b.id
from opplaring_kategorisering_bransje b
where (a.bransje is not null and text(a.bransje) = b.kode);

update avtale_amo_kategorisering a
set kurstype_id = k.id
from opplaring_kategorisering_kurstype k
where (text(a.kurstype) = k.kode);

alter table avtale_amo_kategorisering
    add constraint avtale_amo_kategorisering_bransje_id_fkey
        foreign key (bransje_id)
            references opplaring_kategorisering_bransje (id),
    add constraint avtale_amo_kategorisering_kurstype_id_fkey
        foreign key (kurstype_id)
            references opplaring_kategorisering_kurstype (id);

create index if not exists idx_avtale_amo_kategorisering_bransje_id
    on avtale_amo_kategorisering (bransje_id);
create index if not exists idx_avtale_amo_kategorisering_kurstype_id
    on avtale_amo_kategorisering (kurstype_id);

--- gjennomforing bransje og kurstype
alter table gjennomforing_amo_kategorisering
    add column bransje_id  uuid,
    add column kurstype_id uuid;

update gjennomforing_amo_kategorisering g
set bransje_id = b.id
from opplaring_kategorisering_bransje b
where (g.bransje is not null and text(g.bransje) = b.kode);

update gjennomforing_amo_kategorisering g
set kurstype_id = k.id
from opplaring_kategorisering_kurstype k
where text(g.kurstype) = k.kode;

alter table gjennomforing_amo_kategorisering
    add constraint gjennomforing_amo_kategorisering_bransje_id_fkey
        foreign key (bransje_id)
            references opplaring_kategorisering_bransje (id),
    add constraint gjennomforing_amo_kategorisering_kurstype_id_fkey
        foreign key (kurstype_id)
            references opplaring_kategorisering_kurstype (id);

create index if not exists idx_gjennomforing_amo_kategorisering_bransje_id
    on gjennomforing_amo_kategorisering (bransje_id);
create index if not exists idx_gjennomforing_amo_kategorisering_kurstype_id
    on gjennomforing_amo_kategorisering (kurstype_id);

drop view if exists view_avtale;
drop view if exists view_gjennomforing_avtale_detaljer;
drop view if exists view_gjennomforing_opplaring_kategorisering;
drop view if exists view_avtale_opplaring_kategorisering;
