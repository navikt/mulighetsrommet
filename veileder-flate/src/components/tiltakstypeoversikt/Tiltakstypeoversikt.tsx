import React, { useEffect, useState } from 'react';
import TiltakstypeTabell from '../tabell/TiltakstypeTabell';
import '../../views/tiltakstype-oversikt/TiltakstypeOversikt.less';
import Fuse from 'fuse.js';
import { useAtom } from 'jotai';
import { tiltakstypeOversiktSok } from '../../core/atoms/atoms';
import { Tiltakstype } from 'mulighetsrommet-api-client';

interface TiltaksoversiktProps {
  tiltakstyper: Tiltakstype[];
}

const Tiltakstypeoversikt = (props: TiltaksoversiktProps) => {
  const { tiltakstyper } = props;
  const fuse = new Fuse(tiltakstyper ?? [], {
    keys: ['id', 'navn', 'ingress'],
    shouldSort: true,
    threshold: 0.3,
  });
  const [sok] = useAtom(tiltakstypeOversiktSok);
  const [queriedTiltakstyper, setQueriedTiltakstyper] = useState(tiltakstyper);

  useEffect(() => {
    if (tiltakstyper) {
      if (sok.length > 0) {
        setQueriedTiltakstyper(fuse.search(sok).map(r => r.item));
      } else {
        setQueriedTiltakstyper(tiltakstyper);
      }
    }
  }, [tiltakstyper, sok]);

  return <>{queriedTiltakstyper && <TiltakstypeTabell tiltakstypeliste={queriedTiltakstyper} />}</>;
};

export default Tiltakstypeoversikt;
