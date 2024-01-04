import { ClockDashedIcon } from "@navikt/aksel-icons";
import { Button } from "@navikt/ds-react";
import StandardModal from "../modal/StandardModal";
import styles from "./HistorikkForBrukerModal.module.scss";
import { HistorikkForBrukerModalInnhold } from "./HistorikkForBrukerModalInnhold";

interface Props {
  setHistorikkModalOpen: (state: boolean) => void;
  isHistorikkModalOpen: boolean;
}

export function HistorikkButton({ setHistorikkModalOpen, isHistorikkModalOpen }: Props) {
  const handleClick = () => {
    setHistorikkModalOpen(true);
  };

  return (
    <>
      <Button
        size="small"
        variant="tertiary"
        onClick={handleClick}
        id="historikk_knapp"
        data-testid="historikk_knapp"
        className={styles.historikk_knapp}
      >
        <ClockDashedIcon aria-label="Historikk" />
        Historikk
      </Button>
      <StandardModal
        className={styles.historikk_modal}
        hideButtons
        modalOpen={isHistorikkModalOpen}
        setModalOpen={() => setHistorikkModalOpen(false)}
        heading="Historikk"
        id="historikk_modal"
      >
        <HistorikkForBrukerModalInnhold />
      </StandardModal>
    </>
  );
}
