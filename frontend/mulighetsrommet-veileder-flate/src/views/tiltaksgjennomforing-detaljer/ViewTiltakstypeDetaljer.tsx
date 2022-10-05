import { Dialog, SuccessStroke } from '@navikt/ds-icons';
import { Alert, Button, Link, Loader } from '@navikt/ds-react';
import { useAtom } from 'jotai';
import { useState } from 'react';
import Delemodal, { logDelMedbrukerEvent } from '../../components/modal/delemodal/Delemodal';
import Nokkelinfo from '../../components/nokkelinfo/Nokkelinfo';
import SidemenyDetaljer from '../../components/sidemeny/SidemenyDetaljer';
import TiltaksdetaljerFane from '../../components/tabs/TiltaksdetaljerFane';
import Tilbakeknapp from '../../components/tilbakeknapp/Tilbakeknapp';
import { Tiltakstyper } from '../../core/api/models';
import { useGetTiltaksnummerFraUrl } from '../../core/api/queries/useGetTiltaksnummerFraUrl';
import { useHentBrukerdata } from '../../core/api/queries/useHentBrukerdata';
import { useHentDeltMedBrukerStatus } from '../../core/api/queries/useHentDeltMedbrukerStatus';
import { useHentVeilederdata } from '../../core/api/queries/useHentVeilederdata';
import useTiltaksgjennomforingByTiltaksnummer from '../../core/api/queries/useTiltaksgjennomforingByTiltaksnummer';
import { tiltaksgjennomforingsfilter } from '../../core/atoms/atoms';
import { useHentFnrFraUrl } from '../../hooks/useHentFnrFraUrl';
import { useNavigerTilDialogen } from '../../hooks/useNavigerTilDialogen';
import TiltaksgjennomforingsHeader from '../../layouts/TiltaksgjennomforingsHeader';
import { capitalize, formaterDato } from '../../utils/Utils';
import styles from './ViewTiltaksgjennomforingDetaljer.module.scss';

const whiteListOpprettAvtaleKnapp: Tiltakstyper[] = ['Midlertidig lønnstilskudd'];

const ViewTiltakstypeDetaljer = () => {
  const tiltaksnummer = useGetTiltaksnummerFraUrl();
  const [filter] = useAtom(tiltaksgjennomforingsfilter);
  const fnr = useHentFnrFraUrl();
  const { data: tiltaksgjennomforing, isLoading, isError } = useTiltaksgjennomforingByTiltaksnummer();
  const [delemodalApen, setDelemodalApen] = useState<boolean>(false);
  const brukerdata = useHentBrukerdata();
  const veilederdata = useHentVeilederdata();
  const { getUrlTilDialogen } = useNavigerTilDialogen();
  const veiledernavn = `${capitalize(veilederdata?.data?.fornavn)} ${capitalize(veilederdata?.data?.etternavn)}`;

  const manuellOppfolging = brukerdata.data?.manuellStatus?.erUnderManuellOppfolging;
  const krrStatusErReservert = brukerdata.data?.manuellStatus?.krrStatus?.erReservert;
  const kanDeleMedBruker =
    !manuellOppfolging && !krrStatusErReservert && brukerdata?.data?.manuellStatus?.krrStatus?.kanVarsles;
  const { harDeltMedBruker } = useHentDeltMedBrukerStatus();
  const datoSidenSistDelt = harDeltMedBruker && formaterDato(new Date(harDeltMedBruker!.created_at!!));

  const handleClickApneModal = () => {
    setDelemodalApen(true);
    logDelMedbrukerEvent('Åpnet dialog');
  };

  if (isLoading) {
    return <Loader className={styles.filter_loader} size="xlarge" />;
  }

  if (isError) {
    return <Alert variant="error">Det har skjedd en feil</Alert>;
  }

  if (!tiltaksgjennomforing) {
    return (
      <Alert variant="warning">{`Det finnes ingen tiltaksgjennomføringer med tiltaksnummer "${tiltaksnummer}"`}</Alert>
    );
  }

  const tooltip = () => {
    if (manuellOppfolging)
      return 'Brukeren får manuell oppfølging og kan ikke benytte seg av de digitale tjenestene våre.';
    else if (krrStatusErReservert)
      return 'Brukeren har  reservert seg mot elektronisk kommunikasjon i Kontakt- og reservasjonsregisteret (KRR).';
    else if (harDeltMedBruker) return `Tiltaket ble sist delt med bruker ${datoSidenSistDelt}`;
    else return 'Del tiltak med bruker';
  };

  return (
    <div className={styles.tiltakstypeDetaljer}>
      <div>
        <Tilbakeknapp tilbakelenke={`/${fnr}/#filter=${encodeURIComponent(JSON.stringify(filter))}`} />
        <TiltaksgjennomforingsHeader />
        {tiltaksgjennomforing.tiltakstype.nokkelinfoKomponenter && (
          <Nokkelinfo nokkelinfoKomponenter={tiltaksgjennomforing.tiltakstype.nokkelinfoKomponenter} />
        )}
      </div>
      <div className={styles.sidemeny}>
        <SidemenyDetaljer />
        {whiteListOpprettAvtaleKnapp.includes(tiltaksgjennomforing.tiltakstype.tiltakstypeNavn) && (
          <Button
            onClick={() => {
              alert('Opprett avtale er ikke implementert enda');
            }}
            variant="primary"
            className={styles.deleknapp}
            aria-label="Opprett avtale"
            data-testid="opprettavtaleknapp"
            title={tooltip()}
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
          disabled={!kanDeleMedBruker}
          title={tooltip()}
          icon={harDeltMedBruker && <SuccessStroke title="Suksess" />}
          iconPosition="left"
        >
          {harDeltMedBruker ? `Delt med bruker ${datoSidenSistDelt}` : 'Del med bruker'}
        </Button>
        {harDeltMedBruker ? (
          <div style={{ textAlign: 'center', marginTop: '1rem' }}>
            <Link href={getUrlTilDialogen(harDeltMedBruker.bruker_fnr!!, harDeltMedBruker.dialogId!!)}>
              Åpne i dialogen
              <Dialog />
            </Link>
          </div>
        ) : null}
      </div>
      <TiltaksdetaljerFane />
      <Delemodal
        modalOpen={delemodalApen}
        setModalOpen={() => setDelemodalApen(false)}
        tiltaksgjennomforingsnavn={tiltaksgjennomforing.tiltaksgjennomforingNavn}
        brukerNavn={brukerdata?.data?.fornavn ?? ''}
        chattekst={tiltaksgjennomforing.tiltakstype.delingMedBruker ?? ''}
        veiledernavn={veiledernavn}
      />
    </div>
  );
};

export default ViewTiltakstypeDetaljer;
