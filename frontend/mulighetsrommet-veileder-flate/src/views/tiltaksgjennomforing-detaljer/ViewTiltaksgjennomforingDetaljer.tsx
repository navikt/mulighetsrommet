import { Chat2Icon, CheckmarkIcon } from '@navikt/aksel-icons';
import { Alert, Button } from '@navikt/ds-react';
import { useAtom } from 'jotai';
import {
  Bruker,
  DelMedBruker,
  Innsatsgruppe,
  NavVeileder,
  SanityTiltaksgjennomforing,
  SanityTiltakstype,
} from 'mulighetsrommet-api-client';
import { useState } from 'react';
import { BrukerHarIkke14aVedtakVarsel } from '../../components/ikkeKvalifisertVarsel/BrukerHarIkke14aVedtakVarsel';
import { BrukerKvalifisererIkkeVarsel } from '../../components/ikkeKvalifisertVarsel/BrukerKvalifisererIkkeVarsel';
import { DetaljerJoyride } from '../../components/joyride/DetaljerJoyride';
import { DetaljerOpprettAvtaleJoyride } from '../../components/joyride/DetaljerOpprettAvtaleJoyride';
import Delemodal, { logDelMedbrukerEvent } from '../../components/modal/delemodal/Delemodal';
import Nokkelinfo, { NokkelinfoProps } from '../../components/nokkelinfo/Nokkelinfo';
import { TilgjengelighetsstatusComponent } from '../../components/oversikt/Tilgjengelighetsstatus';
import SidemenyDetaljer from '../../components/sidemeny/SidemenyDetaljer';
import TiltaksdetaljerFane from '../../components/tabs/TiltaksdetaljerFane';
import Tilbakeknapp from '../../components/tilbakeknapp/Tilbakeknapp';
import { logEvent } from '../../core/api/logger';
import { useGetTiltaksgjennomforingIdFraUrl } from '../../core/api/queries/useGetTiltaksgjennomforingIdFraUrl';
import { paginationAtom } from '../../core/atoms/atoms';
import { environments } from '../../env';
import TiltaksgjennomforingsHeader from '../../layouts/TiltaksgjennomforingsHeader';
import { byttTilDialogFlate } from '../../utils/DialogFlateUtils';
import { capitalize, erPreview, formaterDato } from '../../utils/Utils';
import styles from './ViewTiltaksgjennomforingDetaljer.module.scss';

const whiteListOpprettAvtaleKnapp: SanityTiltakstype.arenakode[] = [
  SanityTiltakstype.arenakode.MIDLONTIL,
  SanityTiltakstype.arenakode.ARBTREN,
  SanityTiltakstype.arenakode.VARLONTIL,
  SanityTiltakstype.arenakode.MENTOR,
  SanityTiltakstype.arenakode.INKLUTILS,
  SanityTiltakstype.arenakode.TILSJOBB,
];

type IndividuelleTiltak = (typeof whiteListOpprettAvtaleKnapp)[number];

function tiltakstypeAsStringIsIndividuellTiltakstype(
  arenakode: SanityTiltakstype.arenakode
): arenakode is IndividuelleTiltak {
  return whiteListOpprettAvtaleKnapp.includes(arenakode);
}

function lenkeTilOpprettAvtaleForEnv(): string {
  const env: environments = import.meta.env.VITE_ENVIRONMENT;
  const baseUrl =
    env === 'production'
      ? 'https://tiltaksgjennomforing.intern.nav.no/'
      : 'https://tiltaksgjennomforing.intern.dev.nav.no/';
  return `${baseUrl}tiltaksgjennomforing/opprett-avtale`;
}

function resolveName(ansatt?: NavVeileder) {
  if (!ansatt) {
    return '';
  }

  return [ansatt.fornavn, ansatt.etternavn]
    .filter(part => part !== '')
    .map(capitalize)
    .join(' ');
}

interface Props {
  tiltaksgjennomforing: SanityTiltaksgjennomforing;
  brukerHarRettPaaTiltak: boolean;
  brukersInnsatsgruppe?: Innsatsgruppe;
  innsatsgruppeForGjennomforing: Innsatsgruppe;
  harDeltMedBruker?: DelMedBruker;
  veilederdata: NavVeileder;
  brukerdata: Bruker;
}

const ViewTiltaksgjennomforingDetaljer = ({
  tiltaksgjennomforing,
  harDeltMedBruker,
  brukerHarRettPaaTiltak,
  innsatsgruppeForGjennomforing,
  veilederdata,
  brukerdata,
}: Props) => {
  const gjennomforingsId = useGetTiltaksgjennomforingIdFraUrl();
  const [page] = useAtom(paginationAtom);
  const [delemodalApen, setDelemodalApen] = useState<boolean>(false);
  const veiledernavn = resolveName(veilederdata);
  const datoSidenSistDelt = harDeltMedBruker && formaterDato(new Date(harDeltMedBruker.createdAt!!));

  const handleClickApneModal = () => {
    setDelemodalApen(true);
    logDelMedbrukerEvent('Åpnet dialog');
  };

  if (!tiltaksgjennomforing) {
    return <Alert variant="warning">{`Det finnes ingen tiltaksgjennomføringer med id: "${gjennomforingsId}"`}</Alert>;
  }

  const kanBrukerFaaAvtale = () => {
    const tiltakstypeNavn = tiltaksgjennomforing.tiltakstype.tiltakstypeNavn;
    if (
      tiltaksgjennomforing.tiltakstype?.arenakode &&
      tiltakstypeAsStringIsIndividuellTiltakstype(tiltaksgjennomforing.tiltakstype.arenakode)
    ) {
      const url = lenkeTilOpprettAvtaleForEnv();
      window.open(url, '_blank');
      logEvent('mulighetsrommet.opprett-avtale', { tiltakstype: tiltakstypeNavn });
    }
  };

  const tilgjengelighetsstatusSomNokkelinfo: NokkelinfoProps = {
    nokkelinfoKomponenter: [
      {
        _id: tiltaksgjennomforing._id,
        innhold: (
          <TilgjengelighetsstatusComponent
            status={tiltaksgjennomforing.tilgjengelighetsstatus}
            stengtFra={tiltaksgjennomforing.stengtFra}
            stengtTil={tiltaksgjennomforing.stengtTil}
          />
        ),
        tittel: tiltaksgjennomforing.estimert_ventetid?.toString() ?? '',
        hjelpetekst: 'Tilgjengelighetsstatusen er beregnet ut i fra data som kommer fra Arena',
      },
    ],
  };

  const opprettAvtale =
    !!tiltaksgjennomforing.tiltakstype?.arenakode &&
    tiltakstypeAsStringIsIndividuellTiltakstype(tiltaksgjennomforing.tiltakstype.arenakode) &&
    !erPreview;

  return (
    <>
      <div className={styles.container}>
        <div className={styles.top_wrapper}>
          {!erPreview && (
            <Tilbakeknapp
              tilbakelenke={`/#page=${page}`}
              tekst="Tilbake til tiltaksoversikten"
            />
          )}
          {!erPreview && (
            <>
              <DetaljerJoyride opprettAvtale={opprettAvtale} />
              {opprettAvtale ? <DetaljerOpprettAvtaleJoyride opprettAvtale={opprettAvtale} /> : null}
            </>
          )}
        </div>
        <BrukerKvalifisererIkkeVarsel
          brukerdata={brukerdata}
          brukerHarRettPaaTiltak={brukerHarRettPaaTiltak}
          innsatsgruppeForGjennomforing={innsatsgruppeForGjennomforing}
        />
        <BrukerHarIkke14aVedtakVarsel brukerdata={brukerdata} />
        <div className={styles.tiltaksgjennomforing_detaljer} id="tiltaksgjennomforing_detaljer">
          <div className={styles.tiltakstype_header_maksbredde}>
            <TiltaksgjennomforingsHeader tiltaksgjennomforing={tiltaksgjennomforing} />
            <div className={styles.flex}>
              {tiltaksgjennomforing.tiltakstype.nokkelinfoKomponenter && (
                <div className={styles.nokkelinfo_container}>
                  <Nokkelinfo
                    uuTitle="Se hvordan prosenten er regnet ut"
                    nokkelinfoKomponenter={tiltaksgjennomforing.tiltakstype.nokkelinfoKomponenter}
                  />
                </div>
              )}
              <Nokkelinfo
                data-testid="tilgjengelighetsstatus_detaljside"
                uuTitle="Se hvor data om tilgjengelighetsstatusen er hentet fra"
                nokkelinfoKomponenter={tilgjengelighetsstatusSomNokkelinfo.nokkelinfoKomponenter}
              />
            </div>
          </div>
          <div className={styles.sidemeny}>
            <SidemenyDetaljer tiltaksgjennomforing={tiltaksgjennomforing} />
            <div className={styles.deleknapp_container}>
              {opprettAvtale && (
                <Button
                  onClick={kanBrukerFaaAvtale}
                  variant="primary"
                  className={styles.deleknapp}
                  aria-label="Opprett avtale"
                  data-testid="opprettavtaleknapp"
                  disabled={!brukerHarRettPaaTiltak}
                >
                  Opprett avtale
                </Button>
              )}
              <Button
                onClick={handleClickApneModal}
                variant="secondary"
                className={styles.deleknapp}
                aria-label="Dele"
                data-testid="deleknapp"
                icon={harDeltMedBruker && <CheckmarkIcon title="Suksess" />}
                iconPosition="left"
              >
                {harDeltMedBruker && !erPreview ? `Delt med bruker ${datoSidenSistDelt}` : 'Del med bruker'}
              </Button>
            </div>
            {!brukerdata?.manuellStatus && !erPreview && (
              <Alert
                title="Vi kunne ikke opprette kontakte med KRR og vet derfor ikke om brukeren har reservert seg mot elektronisk kommunikasjon"
                key="alert-innsatsgruppe"
                data-testid="alert-innsatsgruppe"
                size="small"
                variant="error"
                className={styles.alert}
              >
                Kunne ikke å opprette kontakt med Kontakt- og reservasjonsregisteret (KRR).
              </Alert>
            )}
            {harDeltMedBruker && !erPreview && (
              <div className={styles.dialogknapp}>
                <Button
                  size="small"
                  variant="tertiary"
                  onClick={event => byttTilDialogFlate({ event, dialogId: harDeltMedBruker.dialogId!! })}
                >
                  Åpne i dialogen
                  <Chat2Icon />
                </Button>
              </div>
            )}
          </div>
          <TiltaksdetaljerFane tiltaksgjennomforing={tiltaksgjennomforing} />
          <Delemodal
            modalOpen={delemodalApen}
            lukkModal={() => setDelemodalApen(false)}
            tiltaksgjennomforingsnavn={tiltaksgjennomforing.tiltaksgjennomforingNavn}
            brukernavn={erPreview ? '{Navn}' : brukerdata?.fornavn}
            chattekst={tiltaksgjennomforing.tiltakstype.delingMedBruker ?? ''}
            veiledernavn={erPreview ? '{Veiledernavn}' : veiledernavn}
            brukerFnr={brukerdata.fnr}
            tiltaksgjennomforing={tiltaksgjennomforing}
            brukerdata={brukerdata}
            harDeltMedBruker={harDeltMedBruker}
          />
        </div>
      </div>
    </>
  );
};

export default ViewTiltaksgjennomforingDetaljer;
