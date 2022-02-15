import React from 'react';
import { useParams } from 'react-router-dom';
import MainView from '../../layouts/MainView';
import useTiltaksgjennomforingerByTiltakstypeId from '../../hooks/tiltaksgjennomforing/useTiltaksgjennomforingerByTiltakstypeId';
import TiltaksgjennomforingsTabell from '../../components/tabell/TiltaksgjennomforingTabell';
import '../Tiltakstype-tiltaksgjennomforing-detaljer.less';
import { Alert, Loader, Ingress, BodyLong } from '@navikt/ds-react';
import useTiltakstype from '../../hooks/tiltakstype/useTiltakstype';

interface RouteParams {
  id: string;
}

const TiltakstypeDetaljer = () => {
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
    return null;
  }

  const { tittel, ingress, beskrivelse } = tiltakstype.data;

  return (
    <MainView title={tittel} dataTestId="tiltakstype_header" tilbakelenke="./">
      <div className="tiltakstype-detaljer">
        <Ingress data-testid="tiltakstype_ingress">{ingress}</Ingress>
        <BodyLong data-testid="tiltakstype_beskrivelse">{beskrivelse}</BodyLong>
      </div>
      <TiltaksgjennomforingsTabell tiltaksgjennomforinger={tiltaksgjennomforinger.data} />
    </MainView>
  );
};

export default TiltakstypeDetaljer;
