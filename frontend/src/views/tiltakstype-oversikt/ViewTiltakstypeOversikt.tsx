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
import Ikonknapp from '../../components/knapper/Ikonknapp';
import { Filter } from '@navikt/ds-icons';
import InnsatsgruppefilterTags from '../../components/tags/InnsatsgruppefilterTags';
import SearchFieldTag from '../../components/tags/SearchFieldTag';

const ViewTiltakstypeOversikt = () => {
  const [filter] = useAtom(tiltakstypefilter);
  const [sidemenyVisning, setSidemenyVisning] = useAtom(visSidemeny);

  const { data, isFetching, isError } = useTiltakstyper(filter); //isLoading vs isFetching?

  const HiddenIfSidemeny = hiddenIf(Sidemeny);

  const handleClickSkjulSidemeny = () => {
    setSidemenyVisning(!sidemenyVisning);
    sidemenyVisning
      ? (document.getElementById('tiltakstype-oversikt')!.style.gridTemplateColumns = 'auto')
      : (document.getElementById('tiltakstype-oversikt')!.style.gridTemplateColumns = '15rem auto');
  };

  return (
    <div className="tiltakstype-oversikt" id="tiltakstype-oversikt">
      <HiddenIfSidemeny hidden={!sidemenyVisning} handleClickSkjulSidemeny={handleClickSkjulSidemeny} />
      <div className="filtercontainer">
        <Ikonknapp className="filterknapp" handleClick={handleClickSkjulSidemeny}>
          <Filter />
        </Ikonknapp>
        <div className="filtertags">
          <InnsatsgruppefilterTags />
          <SearchFieldTag />
        </div>
      </div>
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
