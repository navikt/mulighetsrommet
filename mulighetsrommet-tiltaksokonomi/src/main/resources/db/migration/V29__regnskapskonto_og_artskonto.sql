alter table bestilling
    rename column tiltakskode to artskonto;

alter table tiltak_kontering_oebs
    rename column tiltakskode to artskonto;

alter table tiltak_kontering_oebs
    rename column tilskuddstype to regnskapskonto;

alter type tilskuddstype rename to regnskapskonto;

alter table bestilling
    add column regnskapskonto text;

-- Vi mangler historiske data på benyttet regnskapskonto, disse blir derfor etterfylt
-- basert på et eget uttrekk med alle tilsagn som ikke er av typen driftstilskudd
update bestilling
set regnskapskonto = 'TILTAK_DRIFTSTILSKUDD';

alter table bestilling
    alter column regnskapskonto set not null;

