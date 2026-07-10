do
$$
    declare
        y text;
    begin
        for y in (select distinct date_part('year', created_at)::text from gjennomforing where lopenummer is null)
            loop
                execute 'create sequence if not exists lopenummer_' || y || '_seq as integer minvalue 10000';
            end loop;
    end
$$;


update gjennomforing g
set lopenummer = sub.year || '/' || nextval('lopenummer_' || sub.year || '_seq')
from (select id, date_part('year', created_at)::text as year
      from gjennomforing
      where lopenummer is null
      order by created_at) sub
where g.id = sub.id;

alter table gjennomforing
    alter lopenummer set not null;
