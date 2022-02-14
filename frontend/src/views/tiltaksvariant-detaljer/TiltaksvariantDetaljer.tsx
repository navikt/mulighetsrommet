import React from 'react';
import { useParams } from 'react-router-dom';
import MainView from '../../layouts/MainView';
import useTiltaksgjennomforingerByTiltaksvariantId from '../../hooks/tiltaksgjennomforing/useTiltaksgjennomforingerByTiltaksvariantId';
import TiltaksgjennomforingsTabell from '../../components/tabell/TiltaksgjennomforingTabell';
import useTiltaksvariant from '../../hooks/tiltaksvariant/useTiltaksvariant';
import '../Tiltaksvariant-tiltaksgjennomforing-detaljer.less';
import { Alert, Loader, Ingress, BodyLong } from '@navikt/ds-react';

interface RouteParams {
  id: string;
}

const TiltaksvariantDetaljer = () => {
  const params = useParams<RouteParams>();
  const id = Number(params.id);
  const tiltaksvariant = useTiltaksvariant(id);
  const tiltaksgjennomforinger = useTiltaksgjennomforingerByTiltaksvariantId(id);

  if (tiltaksvariant.isError) {
    return <Alert variant="error">Det skjedde en feil</Alert>;
  }

  if (tiltaksvariant.isLoading) {
    return <Loader variant="neutral" size="2xlarge" />;
  }

  if (!tiltaksvariant.data) {
    return null;
  }

  const { tittel, ingress, beskrivelse } = tiltaksvariant.data;

  return (
    <MainView title={tittel} dataTestId="tiltaksvariant_header" tilbakelenke="./">
      <div className="tiltaksvariant-detaljer">
        <Ingress data-testid="tiltaksvariant_ingress">{ingress}</Ingress>
        <BodyLong data-testid="tiltaksvariant_beskrivelse">{beskrivelse}</BodyLong>
      </div>
      <TiltaksgjennomforingsTabell tiltaksgjennomforinger={tiltaksgjennomforinger.data} />
    </MainView>
  );
};

export default TiltaksvariantDetaljer;
