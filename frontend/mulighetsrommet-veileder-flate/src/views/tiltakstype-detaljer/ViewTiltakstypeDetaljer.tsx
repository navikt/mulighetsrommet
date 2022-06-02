import React from 'react';
import { useParams } from 'react-router-dom';
import '../ViewTiltakstype-tiltaksgjennomforing-detaljer.less';
import { Alert, Loader } from '@navikt/ds-react';
import useTiltakstype from '../../hooks/tiltakstype/useTiltakstype';
import Tilbakeknapp from '../../components/tilbakeknapp/Tilbakeknapp';
import TiltaksgjennomforingsHeader from '../../layouts/TiltaksgjennomforingsHeader';
import TiltaksdetaljerFane from '../../components/tabs/TiltaksdetaljerFane';
import { Tiltakskode } from '../../../../mulighetsrommet-api-client';
import Statistikk from '../../components/statistikk/Statistikk';
import SidemenyDetaljer from '../../components/sidemeny/SidemenyDetaljer';

interface RouteParams {
  tiltakskode: Tiltakskode;
}

const ViewTiltakstypeDetaljer = () => {
  const params = useParams<'tiltakskode'>() as RouteParams;

  //TODO legg inn tiltaksgjennomføring når den er klar
  const tiltakstype = useTiltakstype(params.tiltakskode);

  if (tiltakstype.isError) {
    return <Alert variant="error">Det har skjedd en feil</Alert>;
  }

  if (tiltakstype.isLoading) {
    return <Loader variant="neutral" size="2xlarge" />;
  }

  if (!tiltakstype.data) {
    return <Alert variant="warning">Nå er det noe rusk i maskineriet...</Alert>;
  }

  const { navn, tiltakskode, innsatsgruppe, fraDato } = tiltakstype.data;

  return (
    <div className="tiltakstype-detaljer">
      <Tilbakeknapp tilbakelenke="/" />
      <div className="tiltakstype-detaljer__info">
        <TiltaksgjennomforingsHeader tiltakstype={navn} />
        <Statistikk
          tittel="Overgang til arbeid"
          hjelpetekst="Her skal det stå litt om hva denne statistikken viser oss"
          statistikktekst="69%"
        />

        <TiltaksdetaljerFane />
      </div>
      <SidemenyDetaljer
        tiltaksnummer={tiltakskode}
        tiltakstype={tiltakskode}
        innsatsgruppe={innsatsgruppe}
        arrangor={'Arrangør'}
        oppstartsdato={fraDato}
        beskrivelse={'lorem ipsum'}
      />
    </div>
  );
};

export default ViewTiltakstypeDetaljer;
