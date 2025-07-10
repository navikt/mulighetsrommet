update avtale
set prismodell = 'FRI'
where prismodell is null;

alter table avtale
    alter prismodell set not null;
