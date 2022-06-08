import React from 'react';
import './ViewTiltaksgjennomforingDetaljer.less';
import Tilbakeknapp from '../../components/tilbakeknapp/Tilbakeknapp';
import TiltaksgjennomforingsHeader from '../../layouts/TiltaksgjennomforingsHeader';
import Statistikk from '../../components/statistikk/Statistikk';
import SidemenyDetaljer from '../../components/sidemeny/SidemenyDetaljer';
import { useParams } from 'react-router-dom';
import TiltaksdetaljerFane from '../../components/tabs/TiltaksdetaljerFane';
import useTiltaksgjennomforingById from '../../hooks/tiltaksgjennomforing/useTiltaksgjennomforingById';
import { Alert, Loader } from '@navikt/ds-react';

const ViewTiltakstypeDetaljer = () => {
  const { tiltaksnummer } = useParams();
  const hentTiltaksgjennomforing = useTiltaksgjennomforingById(parseInt(tiltaksnummer!));

  const tiltaksgjennomforing = hentTiltaksgjennomforing.data;
  const tiltaksgjennomforingLoading = hentTiltaksgjennomforing.isLoading;
  const tiltaksgjennomforingError = hentTiltaksgjennomforing.isError;

  return (
    <>
      {tiltaksgjennomforingLoading && <Loader className="filter-loader" size="xlarge" />}
      {tiltaksgjennomforingError && <Alert variant="error">Det har skjedd en feil</Alert>}
      {tiltaksgjennomforing &&
        tiltaksgjennomforing.map(
          ({
            _id,
            tiltaksgjennomforingNavn,
            oppstart,
            oppstartsdato,
            beskrivelse,
            tiltakstype,
            kontaktinfoArrangor,
            faneinnhold,
            kontaktinfoTiltaksansvarlig,
          }) => (
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
                  faneinnhold={faneinnhold}
                />
              </div>
              <SidemenyDetaljer
                tiltaksnummer={tiltaksnummer!}
                tiltakstype={tiltakstype}
                arrangor={kontaktinfoArrangor.selskapsnavn}
                oppstartsdato={
                  oppstart === 'dato' ? new Intl.DateTimeFormat().format(new Date(oppstartsdato!)) : 'Løpende'
                }
                regelverk="Regelverk"
              />
            </div>
          )
        )}
    </>
  );
};

export default ViewTiltakstypeDetaljer;
