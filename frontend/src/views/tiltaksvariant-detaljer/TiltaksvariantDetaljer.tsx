import React from 'react';
import { useParams, useHistory } from 'react-router-dom';
import MainView from '../../layouts/MainView';
import useTiltaksgjennomforingerByTiltaksvariantId from '../../hooks/tiltaksgjennomforing/useTiltaksgjennomforingerByTiltaksvariantId';
import TiltaksgjennomforingsTabell from '../../components/tabell/TiltaksgjennomforingTabell';
import useTiltaksvariant from '../../hooks/tiltaksvariant/useTiltaksvariant';
import '../Tiltaksvariant-tiltaksgjennomforing-detaljer.less';
import { Alert, Button, Loader, Ingress, BodyLong } from '@navikt/ds-react';
import { ReactComponent as EditIcon } from '../../ikoner/Edit.svg';
import Sidemeny from '../../components/sidemeny/Sidemeny';

interface RouteParams {
  id: string;
}

const TiltaksvariantDetaljer = () => {
  const params = useParams<RouteParams>();
  const id = Number(params.id);
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
    return <Alert variant="warning">Nå er det noe rusk i maskineriet...</Alert>;
  }

  const { tittel, ingress, beskrivelse } = tiltaksvariant.data;

  return (
    <MainView
      title={tittel}
      dataTestId="tiltaksvariant_header"
      tilbakelenke="./"
      contentClassName="tiltaksvariant-detaljer"
    >
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
      <TiltaksgjennomforingsTabell tiltaksgjennomforinger={tiltaksgjennomforinger.data} />
    </MainView>
  );
};

export default TiltaksvariantDetaljer;
