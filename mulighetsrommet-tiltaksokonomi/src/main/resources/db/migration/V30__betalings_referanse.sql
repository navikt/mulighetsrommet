alter table faktura
    rename column kid to betalingsreferanse_kid;

alter table faktura
    add column betalingsreferanse_melding text;

-- Vi endrer kontakten til å kreve enten melding eller kid, men ikke begge.
-- Bare etterfylling av eksisterende fakturaer som har kontonummer, men ikke kid
update faktura
    set betalingsreferanse_melding = beskrivelse
where betalingsreferanse_kid is null and kontonummer is not null;
