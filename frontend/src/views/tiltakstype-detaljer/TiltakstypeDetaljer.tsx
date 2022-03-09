import React from 'react';
import { useParams } from 'react-router-dom';
import MainView from '../../layouts/MainView';
import TiltaksgjennomforingsTabell from '../../components/tabell/TiltaksgjennomforingTabell';
import '../Tiltakstype-tiltaksgjennomforing-detaljer.less';
import { Alert, Loader, Ingress, BodyLong } from '@navikt/ds-react';
import useTiltakstype from '../../hooks/tiltakstype/useTiltakstype';
import useTiltaksgjennomforingerByTiltakskode from '../../hooks/tiltaksgjennomforing/useTiltaksgjennomforingerByTiltakskode';
import { Tiltakskode } from '../../api';

interface RouteParams {
  tiltakskode: string;
}

const TiltakstypeDetaljer = () => {
  const params = useParams<RouteParams>();
  const tiltakskode = Tiltakskode[params.tiltakskode as keyof typeof Tiltakskode];
  const tiltakstype = useTiltakstype(tiltakskode);
  const tiltaksgjennomforinger = useTiltaksgjennomforingerByTiltakskode(tiltakskode);

  if (tiltakstype.isError) {
    return <Alert variant="error">Det skjedde en feil</Alert>;
  }

  if (tiltakstype.isLoading) {
    return <Loader variant="neutral" size="2xlarge" />;
  }

  if (!tiltakstype.data) {
    return null;
  }

  const { navn } = tiltakstype.data;

  return (
    <MainView title={navn} dataTestId="tiltakstype_header" tilbakelenke="./">
      <div className="tiltakstype-detaljer">
        {/* Ingress og beskrivelse er fjernet fra tiltakstype. Dette må vi håndtere i Sanity */}
        <Ingress data-testid="tiltakstype_ingress">INGRESS FRA SANITY</Ingress>
        <BodyLong data-testid="tiltakstype_beskrivelse">BESKRIVELSE FRA SANITY</BodyLong>
      </div>
      <TiltaksgjennomforingsTabell tiltaksgjennomforinger={tiltaksgjennomforinger.data} />
    </MainView>
  );
};

export default TiltakstypeDetaljer;
