import React from 'react';
import { useParams } from 'react-router-dom';
import MainView from '../../layouts/MainView';
import useTiltaksgjennomforing from '../../hooks/tiltaksgjennomforing/useTiltaksgjennomforing';
import '../Tiltaksvariant-tiltaksgjennomforing-detaljer.less';
import { BodyLong } from '@navikt/ds-react';

interface RouteParams {
  tiltaksvariantId: string;
  tiltaksgjennomforingsId: string;
}

const TiltaksgjennomforingDetaljer = () => {
  const { tiltaksgjennomforingsId, tiltaksvariantId }: RouteParams = useParams();

  const tiltaksgjennomforing = useTiltaksgjennomforing(Number(tiltaksgjennomforingsId));

  return (
    <MainView tilbakelenke={`/tiltaksvarianter/${tiltaksvariantId}`} title={tiltaksgjennomforing.data?.tittel}>
      <div className="tiltaksgjennomforing-detaljer">
        <BodyLong>{tiltaksgjennomforing.data?.beskrivelse}</BodyLong>
      </div>
    </MainView>
  );
};

export default TiltaksgjennomforingDetaljer;
