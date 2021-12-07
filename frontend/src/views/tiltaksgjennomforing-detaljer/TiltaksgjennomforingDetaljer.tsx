import React from 'react';
import { useParams } from 'react-router-dom';
import MainView from '../../layouts/MainView';
import { Normaltekst, Systemtittel } from 'nav-frontend-typografi';
import Panel from 'nav-frontend-paneler';
import useTiltaksgjennomforing from '../../hooks/tiltaksgjennomforing/useTiltaksgjennomforing';
import '../Tiltaksvariant-tiltaksgjennomforing-detaljer.less';

interface RouteParams {
  tiltaksvariantId: string;
  tiltaksgjennomforingsId: string;
}

const TiltaksgjennomforingDetaljer = () => {
  const { tiltaksgjennomforingsId, tiltaksvariantId }: RouteParams = useParams();

  const tiltaksgjennomforing = useTiltaksgjennomforing(tiltaksgjennomforingsId);

  return (
    <MainView tilbakelenke={`/tiltaksvarianter/${tiltaksvariantId}`} title={tiltaksgjennomforing.data?.tittel}>
      <div className="tiltaksgjennomforing-detaljer">
        <div className="tiltaksgjennomforing-detaljer__info">
          <Normaltekst>{tiltaksgjennomforing.data?.beskrivelse}</Normaltekst>
        </div>
        <Panel border>
          <Systemtittel>Meny</Systemtittel>
        </Panel>
      </div>
    </MainView>
  );
};

export default TiltaksgjennomforingDetaljer;
