import React, { useEffect, useState } from 'react';
import 'nav-frontend-tabell-style';
import { Tiltaksvariant } from '../../core/domain/Tiltaksvariant';
import Tiltaksvariantliste from './listevisning/Tiltaksvariantliste';
import '../../views/tiltaksvariant-oversikt/TiltaksvariantOversikt.less';
import { tiltaksvariantOversiktSok } from '../../core/atoms/atoms';
import { useAtom } from 'jotai';
import Fuse from 'fuse.js';

interface TiltaksoversiktProps {
  tiltaksvarianter?: Tiltaksvariant[];
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

  return <>{tiltaksvarianter && <Tiltaksvariantliste tiltaksvariantliste={queriedTiltaksvarianter} />}</>;
};

export default Tiltaksvariantoversikt;
