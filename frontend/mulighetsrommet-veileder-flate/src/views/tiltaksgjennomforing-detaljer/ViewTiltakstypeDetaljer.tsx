import React, { useState } from 'react';
import './ViewTiltaksgjennomforingDetaljer.less';
import Tilbakeknapp from '../../components/tilbakeknapp/Tilbakeknapp';
import TiltaksgjennomforingsHeader from '../../layouts/TiltaksgjennomforingsHeader';
import Nokkelinfo from '../../components/nokkelinfo/Nokkelinfo';
import SidemenyDetaljer from '../../components/sidemeny/SidemenyDetaljer';
import TiltaksdetaljerFane from '../../components/tabs/TiltaksdetaljerFane';
import useTiltaksgjennomforingByTiltaksnummer from '../../core/api/queries/useTiltaksgjennomforingByTiltaksnummer';
import { Alert, Loader } from '@navikt/ds-react';
import { useGetTiltaksnummerFraUrl } from '../../core/api/queries/useGetTiltaksnummerFraUrl';
import { useHentFnrFraUrl } from '../../hooks/useHentFnrFraUrl';
import Deleknapp from '../../components/knapper/Deleknapp';
import Delemodal, { logDelMedbrukerEvent } from '../../components/modal/delemodal/Delemodal';
import { useHentBrukerdata } from '../../core/api/queries/useHentBrukerdata';
import { useAtom } from 'jotai';
import { tiltaksgjennomforingsfilter } from '../../core/atoms/atoms';
import { useHentVeilederdata } from '../../core/api/queries/useHentVeilederdata';

const ViewTiltakstypeDetaljer = () => {
  const tiltaksnummer = useGetTiltaksnummerFraUrl();
  const [filter] = useAtom(tiltaksgjennomforingsfilter);
  const fnr = useHentFnrFraUrl();
  const { data: tiltaksgjennomforing, isLoading, isError } = useTiltaksgjennomforingByTiltaksnummer();
  const [delemodalApen, setDelemodalApen] = useState<boolean>(false);
  const brukerdata = useHentBrukerdata();
  const veilederdata = useHentVeilederdata();
  const veiledernavn = `${veilederdata?.data?.fornavn} ${veilederdata?.data?.etternavn}`;

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

  const kanDeleMedBruker =
    !brukerdata.data?.manuellStatus?.erUnderManuellOppfolging &&
    !brukerdata.data?.manuellStatus?.krrStatus?.erReservert &&
    brukerdata?.data?.manuellStatus?.krrStatus?.kanVarsles;

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
        <Deleknapp
          dataTestId="del-med-bruker-button"
          ariaLabel={'Dele'}
          handleClick={handleClickApneModal}
          disabled={!kanDeleMedBruker}
        >
          {kanDeleMedBruker ? (
            'Del med bruker'
          ) : (
            <span title="Bruker er under manuell oppfølging, finnes i Kontakt- og reservasjonsregisteret eller har ikke vært innlogget på NAV.no siste 18 mnd. Brukeren kan dermed ikke kontaktes digitalt.">
              Del med bruker
            </span>
          )}
        </Deleknapp>
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
