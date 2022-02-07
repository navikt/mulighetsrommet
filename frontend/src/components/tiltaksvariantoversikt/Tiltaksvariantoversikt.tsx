import React, { useEffect, useState } from 'react';
import TiltaksvariantTabell from '../tabell/TiltaksvariantTabell';
import '../../views/tiltaksvariant-oversikt/TiltaksvariantOversikt.less';
import Fuse from 'fuse.js';
import { useAtom } from 'jotai';
import { tiltaksvariantOversiktSok } from '../../api/atoms/atoms';
import { Tiltaksvariant } from '../../api';
import { Alert } from '@navikt/ds-react';

interface TiltaksoversiktProps {
  tiltaksvarianter: Tiltaksvariant[];
}

const Tiltaksvariantoversikt = (props: TiltaksoversiktProps) => {
  const { tiltaksvarianter } = props;
  const fuse = new Fuse(tiltaksvarianter ?? [], {
    keys: ['id', 'tittel', 'ingress'],
    shouldSort: true,
    threshold: 0.3,
  });
  const [sok] = useAtom(tiltaksvariantOversiktSok);
  const [queriedTiltaksvarianter, setQueriedTiltaksvarianter] = useState(tiltaksvarianter);

  useEffect(() => {
    if (tiltaksvarianter) {
      if (sok.length > 0) {
        setQueriedTiltaksvarianter(fuse.search(sok).map(r => r.item));
      } else {
        setQueriedTiltaksvarianter(tiltaksvarianter);
      }
    }
  }, [tiltaksvarianter, sok]);

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
