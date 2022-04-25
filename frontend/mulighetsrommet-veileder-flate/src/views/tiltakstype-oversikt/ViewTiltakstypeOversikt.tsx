import React from 'react';
import './ViewTiltakstypeOversikt.less';
import '../../layouts/MainView.less';
import { Alert, Loader } from '@navikt/ds-react';
import Filtermeny from '../../components/filtrering/Filtermeny';
import useTiltakstyper from '../../hooks/tiltakstype/useTiltakstyper';
import TiltakstypeTabell from '../../components/tabell/TiltakstypeTabell';
import { useAtom } from 'jotai';
import InnsatsgruppefilterTags from '../../components/tags/InnsatsgruppefilterTags';
import SearchFieldTag from '../../components/tags/SearchFieldTag';
import { tiltakstypefilter } from '../../core/atoms/atoms';
import { FAKE_DOOR, useFeatureToggles } from '../../api/feature-toggles';

const ViewTiltakstypeOversikt = () => {
  const [filtrertListe] = useAtom(tiltakstypefilter);

  const features = useFeatureToggles();
  const visFakeDoorFeature = features.isSuccess && features.data[FAKE_DOOR];

  const { data, isFetching, isError } = useTiltakstyper(filtrertListe); //isLoading vs isFetching?

  return (
    <>
      {visFakeDoorFeature ? (
        <Alert variant="info">
          Ååååja, så du tror at hvis du trykker på en knapp så skjer det ting automatisk, du da?
        </Alert>
      ) : (
        <div className="tiltakstype-oversikt" id="tiltakstype-oversikt" data-testid="tiltakstype-oversikt">
          <Filtermeny />
          <div className="filtercontainer">
            <div className="filtertags">
              <InnsatsgruppefilterTags />
              <SearchFieldTag />
            </div>
          </div>
          <div className="tiltakstype-oversikt__tiltak">
            {isFetching && !data && <Loader variant="neutral" size="2xlarge" />}
            {data && <TiltakstypeTabell tiltakstypeliste={data} />}
            {isError && <Alert variant="error">En feil oppstod. Vi har problemer med å hente tiltakstypene.</Alert>}
          </div>
        </div>
      )}
    </>
  );
};

export default ViewTiltakstypeOversikt;
