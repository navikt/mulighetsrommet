import React from 'react';
import { useHistory, useParams } from 'react-router-dom';
import MainView from '../../layouts/MainView';
import useTiltaksvariant from '../../hooks/tiltaksvariant/useTiltaksvariant';
import useTiltaksgjennomforingerByTiltaksvariantId from '../../hooks/tiltaksgjennomforing/useTiltaksgjennomforingerByTiltaksvariantId';
import TiltaksgjennomforingsTabell from '../../components/tabell/TiltaksgjennomforingTabell';
import '../Tiltaksvariant-tiltaksgjennomforing-detaljer.less';
import { Alert, Button, Loader, Ingress, BodyLong } from '@navikt/ds-react';
import { ReactComponent as EditIcon } from '../../ikoner/Edit.svg';
import Sidemeny from '../../components/sidemeny/Sidemeny';

interface RouteParams {
  id: string;
}

const TiltaksvariantDetaljer = () => {
  const { id } = useParams<RouteParams>();
  const history = useHistory();
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
        <div className="tiltaksvariant-detaljer__info">
          <Ingress data-testid="tiltaksvariant_ingress">{ingress}</Ingress>
          <BodyLong data-testid="tiltaksvariant_beskrivelse">{beskrivelse}</BodyLong>
        </div>
        <Sidemeny>
          <Button
            variant="primary"
            className="knapp knapp--hoved rediger-knapp"
            data-testid="knapp_rediger-tiltaksvariant"
            onClick={() => history.push(`/tiltaksvarianter/${id}/rediger`)}
          >
            Rediger <EditIcon />
          </Button>
        </Sidemeny>
      </div>
      <TiltaksgjennomforingsTabell tiltaksgjennomforinger={tiltaksgjennomforinger.data} />
    </MainView>
  );
};

export default TiltaksvariantDetaljer;
