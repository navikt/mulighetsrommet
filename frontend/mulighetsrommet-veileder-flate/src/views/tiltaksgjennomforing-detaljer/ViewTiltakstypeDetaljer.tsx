import React from 'react';
import './ViewTiltaksgjennomforingDetaljer.less';
import Tilbakeknapp from '../../components/tilbakeknapp/Tilbakeknapp';
import TiltaksgjennomforingsHeader from '../../layouts/TiltaksgjennomforingsHeader';
import Statistikk from '../../components/statistikk/Statistikk';
import SidemenyDetaljer from '../../components/sidemeny/SidemenyDetaljer';
import { useParams } from 'react-router-dom';
import TiltaksdetaljerFane from '../../components/tabs/TiltaksdetaljerFane';
import useTiltaksgjennomforingById from '../../api/queries/useTiltaksgjennomforingById';
import { Alert, Loader } from '@navikt/ds-react';

const ViewTiltakstypeDetaljer = () => {
  const { tiltaksnummer = '' } = useParams();
  const { data: tiltaksgjennomforing, isLoading, isError } = useTiltaksgjennomforingById(parseInt(tiltaksnummer));

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

  const {
    _id,
    tiltaksgjennomforingNavn,
    oppstart,
    oppstartsdato,
    beskrivelse,
    tiltakstype,
    kontaktinfoArrangor,
    faneinnhold,
    kontaktinfoTiltaksansvarlig,
  } = tiltaksgjennomforing;

  return (
    <div key={_id} className="tiltakstype-detaljer">
      <Tilbakeknapp tilbakelenke="/" />
      <div className="tiltakstype-detaljer__info">
        <TiltaksgjennomforingsHeader
          tiltaksgjennomforingsnavn={tiltaksgjennomforingNavn}
          beskrivelseTiltaksgjennomforing={beskrivelse}
          beskrivelseTiltakstype={tiltakstype.beskrivelse}
        />
        <Statistikk
          tittel="Overgang til arbeid"
          hjelpetekst="Her skal det stå litt om hva denne statistikken viser oss"
          statistikktekst="69%"
        />
        <TiltaksdetaljerFane
          tiltaksgjennomforingTiltaksansvarlig={kontaktinfoTiltaksansvarlig}
          tiltaksgjennomforingArrangorinfo={kontaktinfoArrangor}
          tiltakstype={tiltakstype}
          tiltaksgjennomforing={faneinnhold}
        />
      </div>
      <SidemenyDetaljer
        tiltaksnummer={tiltaksnummer!}
        tiltakstype={tiltakstype}
        arrangor={kontaktinfoArrangor.selskapsnavn}
        oppstartsdato={oppstart === 'dato' ? new Intl.DateTimeFormat().format(new Date(oppstartsdato!)) : 'Løpende'}
      />
    </div>
  );
};

export default ViewTiltakstypeDetaljer;
