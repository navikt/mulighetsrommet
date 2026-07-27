insert into opplaring_kategorisering_kurstype (id, kode, navn, aktiv)
values ('8e294221-bf60-466a-96bd-7c59c338ee5e', 'NORSKOPPLAERING', 'Norskopplæring', true),
       ('19544ff4-25e5-4925-b942-6109b2a98552', 'GRUNNLEGGENDE_FERDIGHETER', 'Grunnleggende ferdigheter', true),
       ('347ef4a1-be8c-47b6-8e67-54244b648a9f', 'FORBEREDENDE_OPPLAERING_FOR_VOKSNE', 'FOV (Forberedende opplæring for voksne)', true),
       ('8c439235-4363-4137-859e-bfa33b0e8f2d', 'BRANSJE_OG_YRKESRETTET', 'Bransje og yrkesrettet', false),
       ('a262e282-2f81-411d-b450-06b7f3d371dc', 'STUDIESPESIALISERING', 'Studiespesialisering', false)
on conflict (id) do nothing;

insert into opplaring_kategorisering_bransje (id, kode, navn)
values ('d9b1c8e0-1c3a-4f5b-9c2e-1a2b3c4d5e6f', 'BYGG_OG_ANLEGG', 'Bygg og anlegg'),
       ('d04dff0d-fdca-4839-9bdc-44c722af5d6f', 'INGENIOR_OG_IKT_FAG', 'Ingeniør- og IKT-fag'),
       ('82bd7ce0-70f1-448b-8773-9015dea613e7', 'HELSE_PLEIE_OG_OMSORG', 'Helse, pleie og omsorg'),
       ('14886bad-a495-420a-9bae-d33e2d88041a', 'BARNE_OG_UNGDOMSARBEID', 'Barne- og ungdomsarbeid'),
       ('a86c1f7a-47c3-4f69-b138-89341107e0eb', 'KONTORARBEID', 'Kontorarbeid'),
       ('e6749d6c-aacf-452d-baf2-d5fb5021912b', 'BUTIKK_OG_SALGSARBEID', 'Butikk- og salgsarbeid'),
       ('4733d7ef-d106-47a4-b335-bfd132c8ad31', 'INDUSTRIARBEID', 'Industriarbeid'),
       ('c8851a31-6362-4ee2-8989-e5da95726076', 'REISELIV_SERVERING_OG_TRANSPORT', 'Reiseliv, servering og transport'),
       ('47c9d5f0-66ea-4e68-949d-86733346ee80', 'SERVICEYRKER_OG_ANNET_ARBEID', 'Serviceyrker og annet arbeid'),
       ('54ccb278-92ea-4835-8566-659e98602905', 'ANDRE_BRANSJER', 'Andre bransjer')
on conflict (id) do nothing;

insert into opplaring_forerkort (id, kode, navn)
values ('810fe1c6-56b0-4e00-8ae6-00fb574299e5', 'A', 'A - Motorsykkel'),
       ('c67006e4-2629-4993-a047-92f31b0db557', 'A1', 'A1 - Lett motorsykkel'),
       ('ed44bd3a-aedb-4225-a3d8-c8f1b95fec5a', 'A2', 'A2 - Mellomtung motorsykkel'),
       ('dee7d6b8-02dc-4b7e-bb3a-fa71cc9248e3', 'AM', 'AM - Moped'),
       ('ee66eb0b-d4a8-4527-800a-135dd3c0d422', 'AM_147', 'AM 147 - Mopedbil'),
       ('79d1a970-e8f0-4ecd-8d5e-e7c8d5f3394c', 'B', 'B - Personbil'),
       ('84a40884-421c-406c-994d-4c4c15ef8bcc', 'B_78', 'B 78 - Personbil med automatgir'),
       ('cdbebefc-2cec-48d0-9c8e-bd464e56cfaa', 'BE', 'BE - Personbil med tilhenger'),
       ('e3fcf1f7-1f20-4fca-bad5-422b7ee0418f', 'C', 'C - Lastebil'),
       ('c65936e4-479f-4c84-b106-6c9ec0cf9aee', 'C1', 'C1 - Lett lastebil'),
       ('69f88a08-e2de-461f-9258-4f8be546104a', 'C1E', 'C1E - Lett lastebil med tilhenger'),
       ('9a85cdeb-2f6d-44f6-bef2-2add850f7b27', 'CE', 'CE - Lastebil med tilhenger'),
       ('e637320c-a5f0-4f7d-ad44-0a7c4654b4c2', 'D', 'D - Buss'),
       ('5d890e23-6800-4574-a05d-24ca81f35a2a', 'D1', 'D1 - Minibuss'),
       ('34d00562-f382-4027-953d-2b6f6bb7e0e5', 'D1E', 'D1E - Minibuss med tilhenger'),
       ('a7376d16-b0da-4140-8e67-c589be2c0ea2', 'DE', 'DE - Buss med tilhenger'),
       ('5b1e1732-a5e8-45ca-955f-548c65d11065', 'S', 'S - Snøscooter'),
       ('53896c05-7650-48ed-bf23-54ae78794eba', 'T', 'T - Traktor')
on conflict (id) do nothing;

insert into opplaring_innhold_element (id, kode, navn)
values ('312a02cd-8330-4c32-ae4b-8e9cde0060fb', 'GRUNNLEGGENDE_FERDIGHETER', 'Grunnleggende ferdigheter'),
       ('0770648f-210e-4b2e-9524-0b87226c8f4b', 'TEORETISK_OPPLAERING', 'Teoretisk opplæring'),
       ('7ee66328-2dd0-4180-9bb3-165e7854f3b6', 'JOBBSOKER_KOMPETANSE', 'Jobbsøkerkompetanse'),
       ('4264da96-3b68-426a-8629-37a2fafafa27', 'PRAKSIS', 'Praksis'),
       ('8f729892-dbb0-448e-ac4c-7169464f955c', 'ARBEIDSMARKEDSKUNNSKAP', 'Arbeidsmarkedskunnskap'),
       ('c645e920-618c-4dcf-a3b7-516f32040e04', 'NORSKOPPLAERING', 'Norskopplæring'),
       ('331d61f7-c957-4d9c-a229-41d1d1b9c675', 'BRANSJERETTET_OPPLARING', 'Bransjerettet opplæring')
on conflict (id) do nothing;
