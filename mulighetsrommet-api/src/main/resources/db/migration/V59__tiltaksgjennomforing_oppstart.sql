create type tiltaksgjennomforing_oppstart as enum ('Dato', 'Lopende');

alter table tiltaksgjennomforing
    add oppstart tiltaksgjennomforing_oppstart;

update tiltaksgjennomforing tg
set oppstart = case
                   when tt.tiltakskode in ('GRUPPEAMO', 'JOBBK', 'GRUFAGYRKE') then 'Dato'
                   else 'Lopende' end::tiltaksgjennomforing_oppstart
from tiltakstype tt
where tt.id = tg.tiltakstype_id;

alter table tiltaksgjennomforing
    alter oppstart set not null;
