import React, { useEffect } from 'react';
import TiltaksvariantTabell from '../tabell/TiltaksvariantTabell';
import '../../views/tiltaksvariant-oversikt/TiltaksvariantOversikt.less';
import Fuse from 'fuse.js';
import { useAtom } from 'jotai';
import { filtreringInnsatsgruppe, filtrerteTiltaksvarianter, tiltaksvariantOversiktSok } from '../../api/atoms/atoms';
import { Tiltaksvariant } from '../../api';
import { Alert } from '@navikt/ds-react';

interface TiltaksoversiktProps {
  tiltaksvarianter: Tiltaksvariant[];
}

const Tiltaksvariantoversikt = ({ tiltaksvarianter }: TiltaksoversiktProps) => {
  const [queriedTiltaksvarianter, setQueriedTiltaksvarianter] = useAtom(filtrerteTiltaksvarianter);
  const [sok] = useAtom(tiltaksvariantOversiktSok);
  const [innsatsgrupper] = useAtom(filtreringInnsatsgruppe);

  const fuseSok = new Fuse(tiltaksvarianter, {
    keys: ['id', 'tittel', 'ingress'],
    shouldSort: true,
    threshold: 0.3,
  });

  //TODO Alle filtere skal fungere avhengig av hverandre dersom flere er valgt.

  useEffect(() => {
    if (innsatsgrupper.length > 0) {
      setQueriedTiltaksvarianter(
        tiltaksvarianter.filter(elem => elem?.innsatsgruppe != null && innsatsgrupper.includes(elem?.innsatsgruppe))
      );
    } else {
      setQueriedTiltaksvarianter(tiltaksvarianter);
    }
  }, [innsatsgrupper, tiltaksvarianter]);

  useEffect(() => {
    sok.length > 0
      ? setQueriedTiltaksvarianter(fuseSok.search(sok).map(r => r.item))
      : setQueriedTiltaksvarianter(tiltaksvarianter);
  }, [sok, tiltaksvarianter]);

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
