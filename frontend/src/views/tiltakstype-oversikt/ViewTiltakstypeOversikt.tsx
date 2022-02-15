import React from 'react';
import './ViewTiltakstypeOversikt.less';
import '../../layouts/MainView.less';
import { Alert, BodyShort, Loader } from '@navikt/ds-react';
import Sidemeny from '../../components/sidemeny/Sidemeny';
import useTiltakstyper from '../../hooks/tiltakstype/useTiltakstyper';
import TiltakstypeTabell from '../../components/tabell/TiltakstypeTabell';
import { useAtom } from 'jotai';
import { tiltakstypefilter, visSidemeny } from '../../api/atoms/atoms';
import hiddenIf from '../../utils/HiddenIf';
import SidemenyKnapp from '../../components/knapper/SidemenyKnapp';
import { Filter } from '@navikt/ds-icons';

const ViewTiltakstypeOversikt = () => {
  const [filter] = useAtom(tiltakstypefilter);
  const [sidemenyApen] = useAtom(visSidemeny);

  const { data, isFetching, isError } = useTiltakstyper(filter); //isLoading vs isFetching?

  const HiddenIfSidemeny = hiddenIf(Sidemeny);

  return (
    <div className="tiltakstype-oversikt">
      <HiddenIfSidemeny hidden={!sidemenyApen} />
      <SidemenyKnapp className="filterknapp">
        <Filter />
      </SidemenyKnapp>
      <div className="tiltakstype-oversikt__tiltak">
        <BodyShort>
          Viser {data?.length} av {data?.length} tiltak
        </BodyShort>
        {isFetching && !data && <Loader variant="neutral" size="2xlarge" />}
        {data && <TiltakstypeTabell tiltakstypeliste={data} />}
        {isError && <Alert variant="error">En feil oppstod. Vi har problemer med å hente tiltakstypene.</Alert>}
      </div>
    </div>
  );
};

export default ViewTiltakstypeOversikt;
