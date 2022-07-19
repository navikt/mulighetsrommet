import React from 'react';
import './ViewTiltaksgjennomforingDetaljer.less';
import Tilbakeknapp from '../../components/tilbakeknapp/Tilbakeknapp';
import TiltaksgjennomforingsHeader from '../../layouts/TiltaksgjennomforingsHeader';
import Statistikk from '../../components/statistikk/Statistikk';
import SidemenyDetaljer from '../../components/sidemeny/SidemenyDetaljer';
import TiltaksdetaljerFane from '../../components/tabs/TiltaksdetaljerFane';
import useTiltaksgjennomforingByTiltaksnummer from '../../api/queries/useTiltaksgjennomforingByTiltaksnummer';
import { Alert, Loader } from '@navikt/ds-react';
import { useGetTiltaksnummerFraUrl } from '../../api/queries/useGetTiltaksnummerFraUrl';
import { useHentFnrFraUrl } from '../../hooks/useHentFnrFraUrl';

const ViewTiltakstypeDetaljer = () => {
  const tiltaksnummer = useGetTiltaksnummerFraUrl();
  const fnr = useHentFnrFraUrl();
  const { data: tiltaksgjennomforing, isLoading, isError } = useTiltaksgjennomforingByTiltaksnummer();

  if (isLoading) {
    return <Loader className="filter-loader" size="xlarge" />;
  }

  if (isError) {
    return <Alert variant="error">Det har skjedd en feil</Alert>;
  }

  if (!tiltaksgjennomforing) {
    return (
      <Alert variant="warning">{`Det finnes ingen tiltaksgjennomføringer med tiltaksnummer "${tiltaksnummer}"`}</Alert>
    );
  }
  return (
    <div className="tiltakstype-detaljer">
      <div className="tiltakstype-detaljer__info">
        <Tilbakeknapp tilbakelenke={`/${fnr}`} />
        <TiltaksgjennomforingsHeader />
        <Statistikk statistikkKomponenter={tiltaksgjennomforing.tiltakstype.statistikkKomponenter} />
      </div>
      <SidemenyDetaljer />
      <TiltaksdetaljerFane />
    </div>
  );
};

export default ViewTiltakstypeDetaljer;
