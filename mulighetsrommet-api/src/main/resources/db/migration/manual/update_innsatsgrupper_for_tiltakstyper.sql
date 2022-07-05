-- Tiltakstyper som ikke er kartlagt enda settes bare til standard innsats by default.

-- Standard innsats
update tiltakstype set innsatsgruppe_id = 1 where tiltakskode like 'DIGIOPPARB';
update tiltakstype set innsatsgruppe_id = 1 where tiltakskode like 'JOBBK';

-- Situasjonsbestemt innsats
update tiltakstype set innsatsgruppe_id = 2 where tiltakskode like 'ARBTREN';
update tiltakstype set innsatsgruppe_id = 2 where tiltakskode like 'AVKLARAG';
update tiltakstype set innsatsgruppe_id = 2 where tiltakskode like 'ENKELAMO';
update tiltakstype set innsatsgruppe_id = 2 where tiltakskode like 'ENKFAGYRKE';
update tiltakstype set innsatsgruppe_id = 2 where tiltakskode like 'INDOPPFAG';
update tiltakstype set innsatsgruppe_id = 2 where tiltakskode like 'INKLUTILS';
update tiltakstype set innsatsgruppe_id = 2 where tiltakskode like 'MENTOR';
update tiltakstype set innsatsgruppe_id = 2 where tiltakskode like 'MIDLONTIL';

-- Spesiell tilpasset innsats
update tiltakstype set innsatsgruppe_id = 3 where tiltakskode like 'ARBFORB';
update tiltakstype set innsatsgruppe_id = 3 where tiltakskode like 'HOYEREUTD';

-- Varig tilpasset innsats
update tiltakstype set innsatsgruppe_id = 4 where tiltakskode like 'VARLONTIL';
update tiltakstype set innsatsgruppe_id = 4 where tiltakskode like 'VASV';
update tiltakstype set innsatsgruppe_id = 4 where tiltakskode like 'VATIAROR';
