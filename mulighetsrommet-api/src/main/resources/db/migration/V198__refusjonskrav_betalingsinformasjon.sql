alter table refusjonskrav
    add column konto_nummer varchar(11) not null default '11111111111';

alter table refusjonskrav
    add column kid varchar(25);
