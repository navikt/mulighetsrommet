alter table totrinnskontroll
    add column behandlet_begrunnelse text,
    add column behandlet_aarsaker text[],
    add column besluttet_begrunnelse text,
    add column besluttet_aarsaker text[];

update totrinnskontroll
set behandlet_begrunnelse = case
        when status in ('TIL_BEHANDLING', 'GODKJENT') then forklaring
    end,
    behandlet_aarsaker = case
        when status in ('TIL_BEHANDLING', 'GODKJENT') then aarsaker
        else array[]::text[]
    end,
    besluttet_begrunnelse = case
        when status in ('SATT_PA_VENT', 'RETURNERT') then forklaring
    end,
    besluttet_aarsaker = case
        when status in ('SATT_PA_VENT', 'RETURNERT') then aarsaker
        else array[]::text[]
    end;

alter table totrinnskontroll
    alter column behandlet_aarsaker set not null,
    alter column besluttet_aarsaker set not null,
    drop column forklaring,
    drop column aarsaker;
