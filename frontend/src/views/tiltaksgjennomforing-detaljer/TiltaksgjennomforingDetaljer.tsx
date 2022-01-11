import React from 'react';
import { useParams } from 'react-router-dom';
import MainView from '../../layouts/MainView';
import useTiltaksgjennomforing from '../../hooks/tiltaksgjennomforing/useTiltaksgjennomforing';
import '../Tiltaksvariant-tiltaksgjennomforing-detaljer.less';
import { BodyLong, Heading, Panel } from '@navikt/ds-react';

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
          <BodyLong>{tiltaksgjennomforing.data?.beskrivelse}</BodyLong>
        </div>
        <Panel border>
          <Heading size="medium">Meny</Heading>
        </Panel>
      </div>
    </MainView>
  );
};

export default TiltaksgjennomforingDetaljer;
