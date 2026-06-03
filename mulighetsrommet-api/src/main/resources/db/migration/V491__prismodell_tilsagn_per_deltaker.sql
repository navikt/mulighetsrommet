alter table prismodell
    alter tilsagn_per_deltaker drop default;

alter table prismodell
    alter tilsagn_per_deltaker drop not null;

alter type prismodell_type add value if not exists 'TILSKUDD_TIL_OPPLAERING';

alter table prismodell
    add column totalbelop integer,
    add column tilskudd   jsonb;

alter type prismodell_type add value if not exists 'INGEN_KOSTNADER';

alter table prismodell
    add column aarsak text;
