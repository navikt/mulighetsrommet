import React, { useEffect, useState } from 'react';
import TiltaksvariantTabell from '../tabell/TiltaksvariantTabell';
import '../../views/tiltaksvariant-oversikt/TiltaksvariantOversikt.less';
import Fuse from 'fuse.js';
import { useAtom } from 'jotai';
import { filtreringInnsatsgruppe, tiltaksvariantOversiktSok } from '../../api/atoms/atoms';
import { Tiltaksvariant } from '../../api';
import { Alert } from '@navikt/ds-react';

interface TiltaksoversiktProps {
  tiltaksvarianter: Tiltaksvariant[];
}

const Tiltaksvariantoversikt = ({ tiltaksvarianter }: TiltaksoversiktProps) => {
  const fuseSok = new Fuse(tiltaksvarianter ?? [], {
    keys: ['id', 'tittel', 'ingress'],
    shouldSort: true,
    threshold: 0.3,
  });

  const [sok] = useAtom(tiltaksvariantOversiktSok);
  const [innsatsgrupper] = useAtom(filtreringInnsatsgruppe);

  const [queriedTiltaksvarianter, setQueriedTiltaksvarianter] = useState(tiltaksvarianter);

  useEffect(() => {
    if (tiltaksvarianter) {
      if (sok.length > 0) {
        setQueriedTiltaksvarianter(fuseSok.search(sok).map(r => r.item));
      }
      if (innsatsgrupper.length > 0) {
        setQueriedTiltaksvarianter(
          tiltaksvarianter.filter(elem => elem?.innsatsgruppe != null && innsatsgrupper.includes(elem?.innsatsgruppe))
        );
      } else {
        setQueriedTiltaksvarianter(tiltaksvarianter);
      }
    }
  }, [tiltaksvarianter, sok, innsatsgrupper]);

  return (
    <div className="tiltaksvariant-oversikt__tabell">
      {queriedTiltaksvarianter ? (
        <TiltaksvariantTabell tiltaksvariantliste={queriedTiltaksvarianter} />
      ) : (
        <Alert variant="error">En feil oppstod. Vi har problemer med å hente tiltaksvariantene.</Alert>
      )}
    </div>
  );
};

export default Tiltaksvariantoversikt;
