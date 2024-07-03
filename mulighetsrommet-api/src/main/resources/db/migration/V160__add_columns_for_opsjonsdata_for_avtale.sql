create type opsjonsmodell as enum (
    'TO_PLUSS_EN',
    'TO_PLUSS_EN_PLUSS_EN',
    'TO_PLUSS_EN_PLUSS_EN_PLUSS_EN',
    'ANNET'
);

alter table avtale
add column opsjon_maks_varighet timestamp;

alter table avtale
add column opsjonsmodell opsjonsmodell;

alter table avtale
add column opsjon_custom_opsjonsmodell_navn text;
