import { Alert, Button, Link, Loader } from '@navikt/ds-react';
import { useAtom } from 'jotai';
import { Ansatt } from 'mulighetsrommet-api-client';
import { useState } from 'react';
import { BrukerHarIkke14aVedtakVarsel } from '../../components/ikkeKvalifisertVarsel/BrukerHarIkke14aVedtakVarsel';
import { BrukerKvalifisererIkkeVarsel } from '../../components/ikkeKvalifisertVarsel/BrukerKvalifisererIkkeVarsel';
import Delemodal, { logDelMedbrukerEvent } from '../../components/modal/delemodal/Delemodal';
import Nokkelinfo, { NokkelinfoProps } from '../../components/nokkelinfo/Nokkelinfo';
import { TilgjengelighetsstatusComponent } from '../../components/oversikt/Tilgjengelighetsstatus';
import SidemenyDetaljer from '../../components/sidemeny/SidemenyDetaljer';
import TiltaksdetaljerFane from '../../components/tabs/TiltaksdetaljerFane';
import Tilbakeknapp from '../../components/tilbakeknapp/Tilbakeknapp';
import { logEvent } from '../../core/api/logger';
import { IndividuellTiltaksType } from '../../core/api/models';
import { useGetTiltaksgjennomforingIdFraUrl } from '../../core/api/queries/useGetTiltaksgjennomforingIdFraUrl';
import { useHentBrukerdata } from '../../core/api/queries/useHentBrukerdata';
import { useHentDeltMedBrukerStatus } from '../../core/api/queries/useHentDeltMedbrukerStatus';
import { useHentVeilederdata } from '../../core/api/queries/useHentVeilederdata';
import useTiltaksgjennomforingById from '../../core/api/queries/useTiltaksgjennomforingById';
import { paginationAtom, tiltaksgjennomforingsfilter } from '../../core/atoms/atoms';
import { environments } from '../../env';
import { useBrukerHarRettPaaTiltak } from '../../hooks/useBrukerHarRettPaaTiltak';
import { useHentFnrFraUrl } from '../../hooks/useHentFnrFraUrl';
import { useNavigerTilDialogen } from '../../hooks/useNavigerTilDialogen';
import TiltaksgjennomforingsHeader from '../../layouts/TiltaksgjennomforingsHeader';
import { capitalize, erPreview, formaterDato } from '../../utils/Utils';
import styles from './ViewTiltaksgjennomforingDetaljer.module.scss';
import { DetaljerJoyride } from '../../components/joyride/DetaljerJoyride';
import { DetaljerOpprettAvtaleJoyride } from '../../components/joyride/DetaljerOpprettAvtaleJoyride';
import { Chat2Icon, CheckmarkIcon } from '@navikt/aksel-icons';
import { useFeatureToggles, VIS_JOYRIDE } from '../../core/api/feature-toggles';

const whiteListOpprettAvtaleKnapp = [
  'Midlertidig lønnstilskudd',
  'Arbeidstrening',
  'Varig lønnstilskudd',
  'Mentor',
  'Inkluderingstilskudd',
  'Sommerjobb',
] as const;

type IndividuelleTiltak = (typeof whiteListOpprettAvtaleKnapp)[number];

function tiltakstypeNavnTilUrlVerdi(tiltakstype: IndividuelleTiltak): IndividuellTiltaksType {
  switch (tiltakstype) {
    case 'Midlertidig lønnstilskudd':
      return 'MIDLERTIDIG_LONNSTILSKUDD';
    case 'Arbeidstrening':
      return 'ARBEIDSTRENING';
    case 'Varig lønnstilskudd':
      return 'VARIG_LONNSTILSKUDD';
    case 'Mentor':
      return 'MENTOR';
    case 'Inkluderingstilskudd':
      return 'INKLUDERINGSTILSKUDD';
    case 'Sommerjobb':
      return 'SOMMERJOBB';
  }
}

function tiltakstypeAsStringIsIndividuellTiltakstype(tiltakstype: string): tiltakstype is IndividuelleTiltak {
  return whiteListOpprettAvtaleKnapp.includes(tiltakstype as any);
}

function lenkeTilOpprettAvtaleForEnv(tiltakstype: IndividuelleTiltak): string {
  const env: environments = import.meta.env.VITE_ENVIRONMENT;
  const baseUrl =
    env === 'production'
      ? 'https://tiltaksgjennomforing.intern.nav.no/'
      : 'https://tiltaksgjennomforing.intern.dev.nav.no/';
  return `${baseUrl}tiltaksgjennomforing/opprett-avtale?type=${tiltakstypeNavnTilUrlVerdi(tiltakstype)}`;
}

function resolveName(ansatt?: Ansatt) {
  return [ansatt?.fornavn ?? '', ansatt?.etternavn ?? '']
    .filter(part => part !== '')
    .map(capitalize)
    .join(' ');
}

const ViewTiltaksgjennomforingDetaljer = () => {
  const gjennomforingsId = useGetTiltaksgjennomforingIdFraUrl();
  const [filter] = useAtom(tiltaksgjennomforingsfilter);
  const [page] = useAtom(paginationAtom);
  const fnr = useHentFnrFraUrl();
  const { data: tiltaksgjennomforing, isLoading, isError } = useTiltaksgjennomforingById();
  const [delemodalApen, setDelemodalApen] = useState<boolean>(false);
  const brukerdata = useHentBrukerdata();
  const veilederdata = useHentVeilederdata();
  const { getUrlTilDialogen } = useNavigerTilDialogen();
  const veiledernavn = resolveName(veilederdata.data);
  const { brukerHarRettPaaTiltak } = useBrukerHarRettPaaTiltak();
  const { harDeltMedBruker } = useHentDeltMedBrukerStatus();
  const datoSidenSistDelt = harDeltMedBruker && formaterDato(new Date(harDeltMedBruker.createdAt!!));
  const features = useFeatureToggles();
  const visJoyride = features.isSuccess && features.data[VIS_JOYRIDE];

  const handleClickApneModal = () => {
    setDelemodalApen(true);
    logDelMedbrukerEvent('Åpnet dialog');
  };

  if (isLoading && !tiltaksgjennomforing) {
    return (
      <div className={styles.filter_loader}>
        <Loader size="xlarge" />
      </div>
    );
  }

  if (isError) {
    return <Alert variant="error">Det har skjedd en feil</Alert>;
  }

  if (!tiltaksgjennomforing) {
    return <Alert variant="warning">{`Det finnes ingen tiltaksgjennomføringer med id: "${gjennomforingsId}"`}</Alert>;
  }

  const kanBrukerFaaAvtale = () => {
    const tiltakstypeNavn = tiltaksgjennomforing.tiltakstype.tiltakstypeNavn;
    if (tiltakstypeAsStringIsIndividuellTiltakstype(tiltakstypeNavn)) {
      const url = lenkeTilOpprettAvtaleForEnv(tiltakstypeNavn);
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
            oppstart={tiltaksgjennomforing.oppstart}
            status={tiltaksgjennomforing.tilgjengelighetsstatus}
          />
        ),
        tittel: tiltaksgjennomforing.estimert_ventetid?.toString() ?? '',
        hjelpetekst: 'Tilgjengelighetsstatusen er beregnet ut i fra data som kommer fra Arena',
      },
    ],
  };

  return (
    <>
      <div className={styles.container}>
        <div className={styles.top_wrapper}>
          {!erPreview && (
            <Tilbakeknapp
              tilbakelenke={`/${fnr}/#filter=${encodeURIComponent(JSON.stringify(filter))}&page=${page}`}
              tekst="Tilbake til tiltaksoversikten"
            />
          )}
          {visJoyride && (
            <DetaljerJoyride
              setDelMedBrukerModal={setDelemodalApen}
              opprettAvtale={
                tiltakstypeAsStringIsIndividuellTiltakstype(tiltaksgjennomforing.tiltakstype.tiltakstypeNavn) &&
                whiteListOpprettAvtaleKnapp.includes(tiltaksgjennomforing.tiltakstype.tiltakstypeNavn) &&
                !erPreview
              }
            />
          )}
        </div>

        <DetaljerOpprettAvtaleJoyride
          opprettAvtale={
            tiltakstypeAsStringIsIndividuellTiltakstype(tiltaksgjennomforing.tiltakstype.tiltakstypeNavn) &&
            whiteListOpprettAvtaleKnapp.includes(tiltaksgjennomforing.tiltakstype.tiltakstypeNavn) &&
            !erPreview
          }
        />
        <BrukerKvalifisererIkkeVarsel />
        <BrukerHarIkke14aVedtakVarsel />
        <div className={styles.tiltaksgjennomforing_detaljer} id="tiltaksgjennomforing_detaljer">
          <div className={styles.tiltakstype_header_maksbredde}>
            <TiltaksgjennomforingsHeader />
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
            <SidemenyDetaljer />
            <div className={styles.deleknapp_container}>
              {tiltakstypeAsStringIsIndividuellTiltakstype(tiltaksgjennomforing.tiltakstype.tiltakstypeNavn) &&
                whiteListOpprettAvtaleKnapp.includes(tiltaksgjennomforing.tiltakstype.tiltakstypeNavn) &&
                !erPreview && (
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
            {!brukerdata.data?.manuellStatus && !erPreview && (
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
                <Link href={getUrlTilDialogen(harDeltMedBruker.norskIdent!!, harDeltMedBruker.dialogId!!)}>
                  Åpne i dialogen
                  <Chat2Icon />
                </Link>
              </div>
            )}
          </div>
          <TiltaksdetaljerFane />
          <Delemodal
            modalOpen={delemodalApen}
            lukkModal={() => setDelemodalApen(false)}
            tiltaksgjennomforingsnavn={tiltaksgjennomforing.tiltaksgjennomforingNavn}
            brukernavn={erPreview ? '{Navn}' : brukerdata?.data?.fornavn}
            chattekst={tiltaksgjennomforing.tiltakstype.delingMedBruker ?? ''}
            veiledernavn={erPreview ? '{Veiledernavn}' : veiledernavn}
          />
        </div>
      </div>
    </>
  );
};

export default ViewTiltaksgjennomforingDetaljer;
