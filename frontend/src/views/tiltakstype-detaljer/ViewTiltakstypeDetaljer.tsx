import React from 'react';
import { useParams } from 'react-router-dom';
import '../ViewTiltakstype-tiltaksgjennomforing-detaljer.less';
import { Alert, Loader } from '@navikt/ds-react';
import useTiltakstype from '../../hooks/tiltakstype/useTiltakstype';
import SidemenyDetaljer from '../../components/sidemeny/SidemenyDetaljer';
import Tilbakeknapp from '../../components/tilbakeknapp/Tilbakeknapp';
import MainViewTitle from '../../layouts/MainViewTitle';
import TiltaksdetaljerFane from '../../components/tabs/TiltaksdetaljerFane';
import Statistikk from '../../components/statistikk/Statistikk';

interface RouteParams {
  id: string;
}

const ViewTiltakstypeDetaljer = () => {
  const params = useParams<RouteParams>();
  const id = Number(params.id);
  const tiltakstype = useTiltakstype(id);

  if (tiltakstype.isError) {
    return <Alert variant="error">Det skjedde en feil</Alert>;
  }

  if (tiltakstype.isLoading) {
    return <Loader variant="neutral" size="2xlarge" />;
  }

  if (!tiltakstype.data) {
    return <Alert variant="warning">Nå er det noe rusk i maskineriet...</Alert>;
  }

  const { tittel, arrangor, innsatsgruppe } = tiltakstype.data;

  return (
    <div className="tiltakstype-detaljer">
      <Tilbakeknapp tilbakelenke="./" />
      <div className="tiltakstype-detaljer__info">
        <MainViewTitle title={tittel} tiltakstype={tittel} arrangor={arrangor} />
        <Statistikk innsatsgruppe={innsatsgruppe} />

        <TiltaksdetaljerFane />
      </div>
      <SidemenyDetaljer tiltaksnavn={tittel} />
    </div>
  );
};

export default ViewTiltakstypeDetaljer;
