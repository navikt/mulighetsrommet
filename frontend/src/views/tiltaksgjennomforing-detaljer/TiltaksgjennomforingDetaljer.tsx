import React from 'react';
import { useParams } from 'react-router-dom';
import MainView from '../../layouts/MainView';
import useTiltaksgjennomforing from '../../hooks/tiltaksgjennomforing/useTiltaksgjennomforing';
import '../Tiltaksvariant-tiltaksgjennomforing-detaljer.less';
import '../../layouts/MainView.less';
import { BodyLong } from '@navikt/ds-react';
import Sidemeny from '../../components/sidemeny/Sidemeny';

interface RouteParams {
  tiltaksvariantId: string;
  tiltaksgjennomforingsId: string;
}

const TiltaksgjennomforingDetaljer = () => {
  const { tiltaksgjennomforingsId, tiltaksvariantId }: RouteParams = useParams();

  const tiltaksgjennomforing = useTiltaksgjennomforing(Number(tiltaksgjennomforingsId));

  return (
    <MainView
      tilbakelenke={`/tiltaksvarianter/${tiltaksvariantId}`}
      title={tiltaksgjennomforing.data?.tittel}
      contentClassName="tiltaksgjennomforing-detaljer"
    >
      <div className="tiltaksgjennomforing-detaljer__info">
        <BodyLong>{tiltaksgjennomforing.data?.beskrivelse}</BodyLong>
      </div>
      <Sidemeny />
    </MainView>
  );
};

export default TiltaksgjennomforingDetaljer;
