alter table gjennomforing
    add column faneinnhold_old jsonb;

update gjennomforing set faneinnhold_old = faneinnhold;
