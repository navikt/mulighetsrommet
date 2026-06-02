alter table prismodell
    alter tilsagn_per_deltaker drop default;

alter table prismodell
    alter tilsagn_per_deltaker drop not null;
