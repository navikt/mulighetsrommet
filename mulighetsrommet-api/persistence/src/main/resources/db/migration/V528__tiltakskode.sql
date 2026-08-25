update tiltakstype
set tiltakskode = 'MIDLERTIDIG_LONNSTILSKUDD'
where tiltakskode = 'MIDLERTIDIG_LONNSTLSKUDD';

update tiltakstype
set tiltakskode = 'VARIG_LONNSTILSKUDD'
where tiltakskode = 'VARIG_LONNSTILSKUD';

update tiltakstype
set tiltakskode = 'INKLUDERINGSTILSKUDD'
where tiltakskode = 'INKLUDERINGSTILSKUD';

update tiltakstype
set tiltakskode = 'FIREARIG_LONNSTILSKUDD'
where tiltakskode = 'FIREARIG_LONNSTILSUDD';
