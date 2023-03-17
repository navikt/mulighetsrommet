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
import { useFeatureToggles, VIS_TILGJENGELIGHETSSTATUS } from '../../core/api/feature-toggles';
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
import { Chat2Icon, CheckmarkIcon } from '@navikt/aksel-icons';

const whiteListOpprettAvtaleKnapp = ['Midlertidig lønnstilskudd'];

function tiltakstypeNavnTilUrlVerdi(tiltakstype: string): IndividuellTiltaksType | '' {
  switch (tiltakstype) {
    case 'Midlertidig lønnstilskudd':
      return 'MIDLERTIDIG_LONNSTILSKUDD';
    default:
      return '';
  }
}

function lenkeTilOpprettAvtaleForEnv(tiltakstype: string): string {
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
  const { data } = useFeatureToggles();
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
    const url = lenkeTilOpprettAvtaleForEnv(tiltaksgjennomforing.tiltakstype.tiltakstypeNavn);
    window.open(url, '_blank');
    logEvent('mulighetsrommet.opprett-avtale');
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
        {!erPreview && (
          <Tilbakeknapp
            tilbakelenke={`/${fnr}/#filter=${encodeURIComponent(JSON.stringify(filter))}&page=${page}`}
            tekst="Tilbake til tiltaksoversikten"
          />
        )}
        <BrukerKvalifisererIkkeVarsel />
        <BrukerHarIkke14aVedtakVarsel />
        <div className={styles.tiltakstype_detaljer}>
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
              {data?.[VIS_TILGJENGELIGHETSSTATUS] ? (
                <Nokkelinfo
                  data-testid="tilgjengelighetsstatus_detaljside"
                  uuTitle="Se hvor data om tilgjengelighetsstatusen er hentet fra"
                  nokkelinfoKomponenter={tilgjengelighetsstatusSomNokkelinfo.nokkelinfoKomponenter}
                />
              ) : null}
            </div>
          </div>
          <div className={styles.sidemeny}>
            <SidemenyDetaljer />
            <div className={styles.deleknapp_container}>
              {whiteListOpprettAvtaleKnapp.includes(tiltaksgjennomforing.tiltakstype.tiltakstypeNavn) && !erPreview && (
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
