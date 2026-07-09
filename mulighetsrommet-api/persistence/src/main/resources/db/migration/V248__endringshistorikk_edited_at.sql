alter table endringshistorikk add column edited_at timestamptz;

update endringshistorikk set edited_at = lower(sys_period);

alter table endringshistorikk
    alter column edited_at set not null,
    drop column sys_period;
