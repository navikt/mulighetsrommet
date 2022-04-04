import React, { useState } from 'react';
import './ViewTiltakstypeOversikt.less';
import '../../layouts/MainView.less';
import { Alert, Button, Heading, Loader } from '@navikt/ds-react';
import Filtermeny from '../../components/filtrering/Filtermeny';
import useTiltakstyper from '../../hooks/tiltakstype/useTiltakstyper';
import TiltakstypeTabell from '../../components/tabell/TiltakstypeTabell';
import { useAtom } from 'jotai';
import Ikonknapp from '../../components/knapper/Ikonknapp';
import { Close, Filter } from '@navikt/ds-icons';
import InnsatsgruppefilterTags from '../../components/tags/InnsatsgruppefilterTags';
import SearchFieldTag from '../../components/tags/SearchFieldTag';
import { tiltakstypefilter, visSidemeny } from '../../core/atoms/atoms';
import { logEvent } from '../../api/logger';
import { ALERT_INFO, FAKE_DOOR, useFeatureToggles } from '../../api/feature-toggles';
import Show from '../../utils/Show';

const ViewTiltakstypeOversikt = () => {
  const [filtrertListe] = useAtom(tiltakstypefilter);
  const [sidemenyVisning, setSidemenyVisning] = useAtom(visSidemeny);

  const [visInfoboks, setVisInfoboks] = useState(true);

  const features = useFeatureToggles();
  const visAlertInfoFeature = features.isSuccess && features.data[ALERT_INFO];
  const visFakeDoorFeature = features.isSuccess && features.data[FAKE_DOOR];

  const { data, isFetching, isError } = useTiltakstyper(filtrertListe); //isLoading vs isFetching?

  const handleClick = () => {
    logEvent('mulighetsrommet-alert-info');
    setVisInfoboks(false);
  };

  const Infotekst = () => {
    return (
      <Alert variant="info" size="small">
        Utvalget av arbeidsmarkedstiltakene du ser er tilpasset din tiltaksregion.
        <Button variant="tertiary" size="small" onClick={handleClick}>
          <Close aria-label="Lukknapp for alertstripe" />
        </Button>
      </Alert>
    );
  };

  const handleClickSkjulSidemeny = () => {
    setSidemenyVisning(!sidemenyVisning);
    sidemenyVisning
      ? (document.getElementById('tiltakstype-oversikt')!.style.gridTemplateColumns = 'auto')
      : (document.getElementById('tiltakstype-oversikt')!.style.gridTemplateColumns = '15rem auto');
  };

  return (
    <>
      {visFakeDoorFeature ? (
        <Alert variant="info">
          Ååååja, så du tror at hvis du trykker på en knapp så skjer det ting automatisk, du da?
        </Alert>
      ) : (
        <div className="tiltakstype-oversikt" id="tiltakstype-oversikt" data-testid="tiltakstype-oversikt">
          <Show if={sidemenyVisning}>
            <Filtermeny handleClickSkjulSidemeny={handleClickSkjulSidemeny} />
          </Show>
          <div className="filtercontainer">
            <Ikonknapp className="filterknapp" handleClick={handleClickSkjulSidemeny} ariaLabel="Filterknapp">
              <Filter aria-label="Filterknapp" />
            </Ikonknapp>
            <div className="filtertags">
              <InnsatsgruppefilterTags />
              <SearchFieldTag />
            </div>
          </div>
          <Show if={visInfoboks && visAlertInfoFeature}>
            <Infotekst />
          </Show>
          <div className="tiltakstype-oversikt__tiltak">
            <Heading level="1" size="xsmall">
              Viser {data?.length} av {data?.length} tiltak
            </Heading>
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
