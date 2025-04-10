create type tilskuddstype as enum ('TILTAK_DRIFTSTILSKUDD', 'TILTAK_INVESTERINGER');

alter table utbetaling
    add column tilskuddstype tilskuddstype;

update utbetaling set tilskuddstype = 'TILTAK_DRIFTSTILSKUDD';

alter table utbetaling alter tilskuddstype set not null;
