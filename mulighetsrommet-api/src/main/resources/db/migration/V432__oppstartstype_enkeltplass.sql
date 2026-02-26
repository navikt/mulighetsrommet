update gjennomforing
set oppstart = 'ENKELTPLASS'
from tiltakstype t
where gjennomforing.tiltakstype_id = t.id
  and t.tiltakskode in (
                        'ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING',
                        'ENKELTPLASS_FAG_OG_YRKESOPPLAERING',
                        'HOYERE_UTDANNING');
