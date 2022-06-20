import React from 'react';
import './ViewTiltaksgjennomforingDetaljer.less';
import Tilbakeknapp from '../../components/tilbakeknapp/Tilbakeknapp';
import TiltaksgjennomforingsHeader from '../../layouts/TiltaksgjennomforingsHeader';
import Statistikk from '../../components/statistikk/Statistikk';
import SidemenyDetaljer from '../../components/sidemeny/SidemenyDetaljer';
import { useParams } from 'react-router-dom';
import TiltaksdetaljerFane from '../../components/tabs/TiltaksdetaljerFane';
import useTiltaksgjennomforingById from '../../api/queries/useTiltaksgjennomforingById';
import { Alert, Loader } from '@navikt/ds-react';

const ViewTiltakstypeDetaljer = () => {
  const { tiltaksnummer = '' } = useParams();
  const { data: tiltaksgjennomforing, isLoading, isError } = useTiltaksgjennomforingById(parseInt(tiltaksnummer));

  if (isLoading) {
    return <Loader className="filter-loader" size="xlarge" />;
  }

  if (isError) {
    return <Alert variant="error">Det har skjedd en feil</Alert>;
  }

  if (!tiltaksgjennomforing) {
    return (
      <Alert variant="warning">{`Det finnes ingen tiltaksgjennomføringer med tiltaksnummer "${tiltaksnummer}"`}</Alert>
    );
  }

  return (
    <div className="tiltakstype-detaljer">
      <Tilbakeknapp tilbakelenke="/" />
      <div className="tiltakstype-detaljer__info">
        <TiltaksgjennomforingsHeader tiltaksgjennomforing={tiltaksgjennomforing} />
        <Statistikk
          tittel="Overgang til arbeid"
          hjelpetekst="Her skal det stå litt om hva denne statistikken viser oss"
          statistikktekst="69%"
        />
        <TiltaksdetaljerFane tiltaksgjennomforing={tiltaksgjennomforing} />
      </div>
      <SidemenyDetaljer tiltaksgjennomforing={tiltaksgjennomforing} />
    </div>
  );
};

export default ViewTiltakstypeDetaljer;
