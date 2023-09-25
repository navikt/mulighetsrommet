import { StatusModal } from "../StatusModal";
import { LoaderModal } from "../LoaderModal";
import { useState } from "react";
import { useHentBrukerdata } from "../../../core/api/queries/useHentBrukerdata";

interface KanIkkeDeleMedBrukerModalProps {
  modalOpen: boolean;
  lukkModal: () => void;
  manuellOppfolging: boolean;
  krrStatusErReservert: boolean;
  manuellStatus: boolean;
  kanIkkeDeleMedBruker: boolean;
}

export const KanIkkeDeleMedBrukerModal = ({
  modalOpen,
  lukkModal,
  manuellOppfolging,
  krrStatusErReservert,
  manuellStatus,
  kanIkkeDeleMedBruker,
}: KanIkkeDeleMedBrukerModalProps) => {
  const brukerdata = useHentBrukerdata();

  const [, setData] = useState(brukerdata);
  const [loaderModalApen, setLoaderModalApen] = useState(false);
  const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

  const provIgjen = async () => {
    setData(brukerdata);
    setLoaderModalApen(true);
    await sleep(500);
    setLoaderModalApen(false);
  };

  const feilmelding = () => {
    if (manuellOppfolging)
      return "Brukeren er under manuell oppfølging og kan derfor ikke benytte seg av våre digitale tjenester.";
    else if (krrStatusErReservert)
      return "Brukeren har reservert seg mot elektronisk kommunikasjon i Kontakt- og reservasjonsregisteret (KRR).";
    else if (manuellStatus)
      return "Vi kunne ikke opprette kontakt med KRR og vet derfor ikke om brukeren har reservert seg mot elektronisk kommunikasjon.";
    else if (kanIkkeDeleMedBruker)
      return "Brukeren er reservert mot elektronisk kommunikasjon i KRR og er under manuell oppfølging. Vi kan derfor ikke kommunisere digitalt med denne brukeren.";
    else return "Det har oppstått en feil. Vennligst prøv igjen senere.";
  };

  const heading =
    manuellOppfolging || krrStatusErReservert || manuellStatus || kanIkkeDeleMedBruker
      ? "Kunne ikke dele tiltaket"
      : null;

  return (
    <>
      <StatusModal
        modalOpen={modalOpen}
        onClose={lukkModal}
        ikonVariant="warning"
        heading={heading}
        text={feilmelding()}
        primaryButtonText={manuellStatus ? "Prøv igjen" : "OK"}
        primaryButtonOnClick={() => (manuellStatus ? provIgjen() : lukkModal())}
        secondaryButtonText={manuellStatus ? "Avbryt" : null}
        secondaryButtonOnClick={() => lukkModal()}
      />
      <LoaderModal lukkModal={lukkModal} modalOpen={loaderModalApen} />
    </>
  );
};
