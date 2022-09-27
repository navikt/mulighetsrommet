import React, { useState } from 'react';
import './ViewTiltaksgjennomforingDetaljer.less';
import Tilbakeknapp from '../../components/tilbakeknapp/Tilbakeknapp';
import TiltaksgjennomforingsHeader from '../../layouts/TiltaksgjennomforingsHeader';
import Nokkelinfo from '../../components/nokkelinfo/Nokkelinfo';
import SidemenyDetaljer from '../../components/sidemeny/SidemenyDetaljer';
import TiltaksdetaljerFane from '../../components/tabs/TiltaksdetaljerFane';
import useTiltaksgjennomforingByTiltaksnummer from '../../core/api/queries/useTiltaksgjennomforingByTiltaksnummer';
import { Alert, Button, Loader } from '@navikt/ds-react';
import { useGetTiltaksnummerFraUrl } from '../../core/api/queries/useGetTiltaksnummerFraUrl';
import { useHentFnrFraUrl } from '../../hooks/useHentFnrFraUrl';
import Delemodal, { logDelMedbrukerEvent } from '../../components/modal/delemodal/Delemodal';
import { useHentBrukerdata } from '../../core/api/queries/useHentBrukerdata';
import { useAtom } from 'jotai';
import { tiltaksgjennomforingsfilter } from '../../core/atoms/atoms';
import { useHentVeilederdata } from '../../core/api/queries/useHentVeilederdata';
import { capitalize, formaterDato } from '../../utils/Utils';
import { SuccessStroke } from '@navikt/ds-icons';
import { useHentDeltMedBrukerStatus } from '../../core/api/queries/useHentDeltMedbrukerStatus';

const ViewTiltakstypeDetaljer = () => {
  const tiltaksnummer = useGetTiltaksnummerFraUrl();
  const [filter] = useAtom(tiltaksgjennomforingsfilter);
  const fnr = useHentFnrFraUrl();
  const { data: tiltaksgjennomforing, isLoading, isError } = useTiltaksgjennomforingByTiltaksnummer();
  const [delemodalApen, setDelemodalApen] = useState<boolean>(false);
  const brukerdata = useHentBrukerdata();
  const veilederdata = useHentVeilederdata();
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

  const tooltip = () => {
    if (manuellOppfolging)
      return 'Brukeren får manuell oppfølging og kan ikke benytte seg av de digitale tjenestene våre.';
    else if (krrStatusErReservert)
      return 'Brukeren har  reservert seg mot elektronisk kommunikasjon i Kontakt- og reservasjonsregisteret (KRR).';
    else if (harDeltMedBruker) return `Tiltaket ble sist delt med bruker ${datoSidenSistDelt}`;
    else return 'Del tiltak med bruker';
  };

  return (
    <div className="tiltakstype-detaljer">
      <div className="tiltakstype-detaljer__info">
        <Tilbakeknapp tilbakelenke={`/${fnr}/#filter=${encodeURIComponent(JSON.stringify(filter))}`} />
        <TiltaksgjennomforingsHeader />
        {tiltaksgjennomforing.tiltakstype.nokkelinfoKomponenter && (
          <Nokkelinfo nokkelinfoKomponenter={tiltaksgjennomforing.tiltakstype.nokkelinfoKomponenter} />
        )}
      </div>
      <div className="tiltakstype-detaljer__sidemeny">
        <SidemenyDetaljer />
        <Button
          onClick={handleClickApneModal}
          variant="secondary"
          className="deleknapp"
          aria-label="Dele"
          data-testid="deleknapp"
          disabled={!kanDeleMedBruker}
          title={tooltip()}
          icon={harDeltMedBruker && <SuccessStroke />}
          iconPosition="left"
        >
          {harDeltMedBruker ? `Delt med bruker ${datoSidenSistDelt}` : 'Del med bruker'}
        </Button>
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
