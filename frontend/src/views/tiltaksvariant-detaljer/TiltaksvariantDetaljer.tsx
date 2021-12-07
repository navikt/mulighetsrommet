import React from 'react';
import { useParams } from 'react-router-dom';
import AlertStripe from 'nav-frontend-alertstriper';
import MainView from '../../layouts/MainView';
import Link from '../../components/link/Link';
import { Ingress, Normaltekst, Systemtittel } from 'nav-frontend-typografi';
import Panel from 'nav-frontend-paneler';
import useTiltaksvariant from '../../hooks/tiltaksvariant/useTiltaksvariant';
import useTiltaksgjennomforingerByTiltaksvariantId from '../../hooks/tiltaksgjennomforing/useTiltaksgjennomforingerByTiltaksvariantId';
import TiltaksgjennomforingsTabell from './components/TiltaksgjennomforingTabell';
import '../Tiltaksvariant-tiltaksgjennomforing-detaljer.less';

interface RouteParams {
  id: string;
}

const TiltaksvariantDetaljer = () => {
  const { id } = useParams<RouteParams>();

  const tiltaksvariant = useTiltaksvariant(id);
  const tiltaksgjennomforinger = useTiltaksgjennomforingerByTiltaksvariantId(id);

  if (tiltaksvariant.isError) {
    return <AlertStripe type="feil">Det skjedde en feil</AlertStripe>;
  }

  if (!tiltaksvariant.data) {
    // TODO: loading
    return null;
  }

  const { tittel, ingress, beskrivelse } = tiltaksvariant.data;

  return (
    <MainView title={tittel} dataTestId="tiltaksvariant_header">
      <div className="tiltaksvariant-detaljer">
        <div className="tiltaksvariant-detaljer__info">
          <Ingress data-testid="tiltaksvariant_ingress">{ingress}</Ingress>
          <Normaltekst data-testid="tiltaksvariant_beskrivelse">{beskrivelse}</Normaltekst>
        </div>
        <Panel border>
          <Systemtittel>Meny</Systemtittel>
          <Link
            to={`/tiltaksvarianter/${id}/rediger`}
            className="knapp knapp--hoved rediger-knapp"
            data-testid="knapp_rediger-tiltaksvariant"
          >
            Rediger
          </Link>
        </Panel>
      </div>
      <TiltaksgjennomforingsTabell tiltaksgjennomforinger={tiltaksgjennomforinger.data} />
    </MainView>
  );
};

export default TiltaksvariantDetaljer;
