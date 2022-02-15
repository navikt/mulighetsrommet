import React from 'react';
import { useParams } from 'react-router-dom';
import MainView from '../../layouts/MainView';
import TiltaksgjennomforingsTabell from '../../components/tabell/TiltaksgjennomforingTabell';
import '../ViewTiltakstype-tiltaksgjennomforing-detaljer.less';
import { Alert, Loader, Ingress, BodyLong } from '@navikt/ds-react';
import useTiltaksgjennomforingerByTiltakstypeId from '../../hooks/tiltaksgjennomforing/useTiltaksgjennomforingerByTiltakstypeId';
import useTiltakstype from '../../hooks/tiltakstype/useTiltakstype';

interface RouteParams {
  id: string;
}

const ViewTiltakstypeDetaljer = () => {
  const params = useParams<RouteParams>();
  const id = Number(params.id);
  const tiltakstype = useTiltakstype(id);
  const tiltaksgjennomforinger = useTiltaksgjennomforingerByTiltakstypeId(id);

  if (tiltakstype.isError) {
    return <Alert variant="error">Det skjedde en feil</Alert>;
  }

  if (tiltakstype.isLoading) {
    return <Loader variant="neutral" size="2xlarge" />;
  }

  if (!tiltakstype.data) {
    return <Alert variant="warning">Nå er det noe rusk i maskineriet...</Alert>;
  }

  const { tittel, ingress, beskrivelse } = tiltakstype.data;

  return (
    <MainView title={tittel} dataTestId="tiltakstype_header" tilbakelenke="./" contentClassName="tiltakstype-detaljer">
      <div className="tiltakstype-detaljer__info">
        <Ingress data-testid="tiltakstype_ingress">{ingress}</Ingress>
        <BodyLong data-testid="tiltakstype_beskrivelse">{beskrivelse}</BodyLong>
      </div>
      <TiltaksgjennomforingsTabell tiltaksgjennomforinger={tiltaksgjennomforinger.data} />
    </MainView>
  );
};

export default ViewTiltakstypeDetaljer;
