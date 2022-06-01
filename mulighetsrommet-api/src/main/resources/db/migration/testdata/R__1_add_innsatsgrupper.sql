insert into innsatsgruppe (id, navn)
values (1, 'Standardinnsats'),
       (2, 'Situasjonsbestemt innsats'),
       (3, 'Spesielt tilpasset innsats'),
       (4, 'Varig tilpasset innsats')
on conflict (id) do update set navn      = excluded.navn;
