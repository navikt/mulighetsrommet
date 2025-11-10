alter table avtale
    add column faneinnhold_old jsonb;

update avtale set faneinnhold_old = faneinnhold;
