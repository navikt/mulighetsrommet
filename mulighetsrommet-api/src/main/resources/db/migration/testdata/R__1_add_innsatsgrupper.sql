insert into innsatsgruppe (id, tittel, beskrivelse)
values (1, 'Standardinnsats', 'Gode muligheter'),
       (2, 'Situasjonsbestemt innsats', 'Trenger veiledning'),
       (3, 'Spesielt tilpasset innsats', 'Trenger veiledning, nedsatt arbeidsevne'),
       (4, 'Varig tilpasset innsats', 'Jobbe delvis eller liten mulighet til å jobbe')
on conflict (id) do update set tittel      = excluded.tittel,
                               beskrivelse = excluded.beskrivelse;
