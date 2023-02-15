import { StatusModal } from './StatusModal';
import { useHentBrukerdata } from '../../../core/api/queries/useHentBrukerdata';
import { useState } from 'react';
import { LoaderModal } from '../loaderModal/LoaderModal';

interface KanIkkeDeleMedBrukerModalProps {
  lukkModal: () => void;
  manuellOppfolging: boolean;
  krrStatusErReservert: boolean;
  manuellStatus: boolean;
  kanIkkeDeleMedBruker: boolean;
}

export const KanIkkeDeleMedBrukerModal = ({
  lukkModal,
  manuellOppfolging,
  krrStatusErReservert,
  manuellStatus,
  kanIkkeDeleMedBruker,
}: KanIkkeDeleMedBrukerModalProps) => {
  const brukerdata = useHentBrukerdata();
  const [_, setData] = useState(brukerdata);
  const [loaderModalApen, setLoaderModalApen] = useState(false);
  const sleep = (ms: number) => new Promise(r => setTimeout(r, ms));

  const feilmelding = () => {
    if (manuellOppfolging)
      return 'Brukeren er under manuell oppfølging og kan derfor ikke benytte seg av våre digitale tjenester.';
    else if (krrStatusErReservert)
      return 'Brukeren har reservert seg mot elektronisk kommunikasjon i Kontakt- og reservasjonsregisteret (KRR).';
    else if (manuellStatus)
      return 'Vi kunne ikke opprette kontakt med KRR og vet derfor ikke om brukeren har reservert seg mot elektronisk kommunikasjon.';
    else if (kanIkkeDeleMedBruker)
      return 'Brukeren er reservert mot elektronisk kommunikasjon i KRR og er under manuell oppfølging. Vi kan derfor ikke kommunisere digitalt med denne brukeren.';
    else return 'Det har oppstått en feil. Vennligst prøv igjen senere.';
  };

  const heading =
    manuellOppfolging || krrStatusErReservert || manuellStatus || kanIkkeDeleMedBruker
      ? 'Kunne ikke dele tiltaket'
      : null;

  const provIgjen = async () => {
    setData(brukerdata);
    setLoaderModalApen(true);
    await sleep(500); //ja, det skal være sleep her fordi brukerne skal se at brukerdata hentes på nytt
    setLoaderModalApen(false);
  };

  return (
    <>
      <StatusModal
        ikonVariant="warning"
        heading={heading}
        text={feilmelding()}
        primaryButtonText={manuellStatus ? 'Prøv igjen' : 'OK'}
        primaryButtonOnClick={() => (manuellStatus ? provIgjen() : lukkModal())}
        secondaryButtonText={manuellStatus ? 'Avbryt' : null}
        secondaryButtonOnClick={() => lukkModal()}
      />
      <LoaderModal lukkModal={lukkModal} modalOpen={loaderModalApen} />
    </>
  );
};
