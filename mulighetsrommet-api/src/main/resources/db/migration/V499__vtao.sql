update tiltakstype
set tiltakskode = 'VARIG_TILRETTELAGT_ARBEID_ORDINAER',
    navn        = 'Varig tilrettelagt arbeid i ordinær virksomhet'
where tiltakskode = 'TILPASSET_JOBBSTOTTE';

update prismodell
set system_id = 'VARIG_TILRETTELAGT_ARBEID_ORDINAER'
where system_id = 'TILPASSET_JOBBSTOTTE';
