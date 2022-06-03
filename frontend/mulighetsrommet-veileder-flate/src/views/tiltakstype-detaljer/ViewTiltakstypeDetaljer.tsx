import React, { useEffect, useState } from 'react';
import '../ViewTiltakstype-tiltaksgjennomforing-detaljer.less';
import Tilbakeknapp from '../../components/tilbakeknapp/Tilbakeknapp';
import TiltaksgjennomforingsHeader from '../../layouts/TiltaksgjennomforingsHeader';
import Statistikk from '../../components/statistikk/Statistikk';
import SidemenyDetaljer from '../../components/sidemeny/SidemenyDetaljer';
import { useParams } from 'react-router-dom';
import { client } from '../../sanityClient';
import TiltaksdetaljerFane from '../../components/tabs/TiltaksdetaljerFane';

const ViewTiltakstypeDetaljer = () => {
  const { tiltaksnummer } = useParams();
  // const tiltaksgjennomforing = useTiltaksgjennomforingDetaljer(parseInt(tiltaksnummer!));
  // console.log('tiltak', tiltaksgjennomforing);
  //
  // const { tiltaksgjennomforingNavn, arrangor } = tiltaksgjennomforing;

  //TODO alt dette skal ryddes inn i egne hooks
  const [tiltaksgjennomforinger, setTiltaksgjennomforinger] = useState([]);

  useEffect(() => {
    client
      .fetch(
        `*[_type == "tiltaksgjennomforing" && tiltaksnummer == ${tiltaksnummer}]{ 
        _id,
        tiltaksgjennomforingNavn,
        beskrivelse,
        tiltaksnummer,
        lokasjon,
        oppstart,
        oppstartsdato,
        enheter{
          fylke,
          asker,
          fredrikstad,
          indreOstfold,
          lillestrom,
          ringsaker,
          sarpsborg,
          skiptvedtMarker,
          steinkjer,
          trondheim
        },
        faneinnhold{
          forHvemInfoboks,
          detaljerOgInnholdInfoboks,
          pameldingOgVarighetInfoboks,
          forHvem,
          detaljerOgInnhold,
          pameldingOgVarighet,
        },
        kontaktinfoArrangor->,
        kontaktinfoTiltaksansvarlig->,
        tiltakstype->}`
      )
      .then(data => setTiltaksgjennomforinger(data));
  }, []);

  return (
    <>
      {tiltaksgjennomforinger.map(
        ({
          _id,
          tiltaksgjennomforingNavn,
          oppstart,
          oppstartsdato,
          beskrivelse,
          tiltakstype: { tiltakstypeNavn, innsatsgruppe },
          kontaktinfoArrangor: { selskapsnavn },
          faneinnhold: {
            forHvem,
            detaljerOgInnhold,
            pameldingOgVarighet,
            kontaktinfo,
            forHvemInfoboks,
            detaljerOgInnholdInfoboks,
            pameldingOgVarighetInfoboks,
          },
        }) => (
          <div key={_id} className="tiltakstype-detaljer">
            <Tilbakeknapp tilbakelenke="/" />
            <div className="tiltakstype-detaljer__info">
              <TiltaksgjennomforingsHeader
                tiltaksgjennomforingsnavn={tiltaksgjennomforingNavn}
                beskrivelse={beskrivelse}
              />
              <Statistikk
                tittel="Overgang til arbeid"
                hjelpetekst="Her skal det stå litt om hva denne statistikken viser oss"
                statistikktekst="69%"
              />
              <TiltaksdetaljerFane
                forHvemAlert={forHvemInfoboks}
                detaljerOgInnholdAlert={detaljerOgInnholdInfoboks}
                pameldingOgVarighetAlert={pameldingOgVarighetInfoboks}
                forHvem={forHvem}
                detaljerOgInnhold={detaljerOgInnhold}
                pameldingOgVarighet={pameldingOgVarighet}
                kontaktinfo={kontaktinfo}
              />
            </div>
            <SidemenyDetaljer
              tiltaksnummer={tiltaksnummer!}
              tiltakstype={tiltakstypeNavn}
              innsatsgruppe={innsatsgruppe}
              arrangor={selskapsnavn}
              oppstartsdato={
                oppstart === 'dato' ? new Intl.DateTimeFormat().format(new Date(oppstartsdato)) : 'Løpende'
              }
              beskrivelse={'beskrivelse'}
            />
          </div>
        )
      )}
    </>
  );
};

export default ViewTiltakstypeDetaljer;
