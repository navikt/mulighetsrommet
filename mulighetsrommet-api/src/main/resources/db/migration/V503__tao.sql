
update tiltakstype
set tiltakskode = 'TILRETTELAGT_ARBEID_ORDINAER',
    navn        = 'Tilrettelagt arbeid i ordinær virksomhet'
where tiltakskode = 'VARIG_TILRETTELAGT_ARBEID_ORDINAER';

update prismodell
set system_id = 'TILRETTELAGT_ARBEID_ORDINAER'
where system_id = 'VARIG_TILRETTELAGT_ARBEID_ORDINAER';
