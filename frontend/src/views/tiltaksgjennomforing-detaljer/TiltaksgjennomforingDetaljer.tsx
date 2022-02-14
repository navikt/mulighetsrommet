import React from 'react';
import { useParams } from 'react-router-dom';
import MainView from '../../layouts/MainView';
import useTiltaksgjennomforing from '../../hooks/tiltaksgjennomforing/useTiltaksgjennomforing';
import '../Tiltakstype-tiltaksgjennomforing-detaljer.less';
import { BodyLong } from '@navikt/ds-react';

interface RouteParams {
  tiltakstypeId: string;
  tiltaksgjennomforingsId: string;
}

const TiltaksgjennomforingDetaljer = () => {
  const { tiltaksgjennomforingsId, tiltakstypeId }: RouteParams = useParams();

  const tiltaksgjennomforing = useTiltaksgjennomforing(Number(tiltaksgjennomforingsId));

  return (
    <MainView tilbakelenke={`/tiltakstyper/${tiltakstypeId}`} title={tiltaksgjennomforing.data?.tittel}>
      <div className="tiltaksgjennomforing-detaljer">
        <BodyLong>{tiltaksgjennomforing.data?.beskrivelse}</BodyLong>
      </div>
    </MainView>
  );
};

export default TiltaksgjennomforingDetaljer;
