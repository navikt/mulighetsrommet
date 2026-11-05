alter table faktura
    rename column kid to betalingsreferanse_kid;

alter table faktura
    add column betalingsreferanse_melding text;

-- Vi endrer kontakten til å kreve enten melding eller kid, men ikke begge.
update faktura
    set betalingsreferanse_melding = beskrivelse
where betalingsreferanse_kid is null;
