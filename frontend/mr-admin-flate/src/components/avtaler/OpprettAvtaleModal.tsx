import { Heading, Modal } from "@navikt/ds-react";
import { Avtale, Tiltakstypestatus } from "mulighetsrommet-api-client";
import { useEffect } from "react";
import { useHentAnsatt } from "../../api/ansatt/useHentAnsatt";
import { useAlleEnheter } from "../../api/enhet/useAlleEnheter";
import { useTiltakstyper } from "../../api/tiltakstyper/useTiltakstyper";
import { Laster } from "../laster/Laster";
import styles from "./Modal.module.scss";
import { OpprettAvtaleContainer } from "./OpprettAvtaleContainer";

interface OpprettAvtaleModalProps {
  modalOpen: boolean;
  onClose: () => void;
  handleForm?: () => void;
  handleCancel?: () => void;
  shouldCloseOnOverlayClick?: boolean;
  avtale?: Avtale;
}

const OpprettAvtaleModal = ({
  modalOpen,
  onClose,
  handleCancel,
  avtale,
}: OpprettAvtaleModalProps) => {
  const { data: tiltakstyper, isLoading: isLoadingTiltakstyper } =
    useTiltakstyper(
      {
        status: Tiltakstypestatus.AKTIV,
      },
      1
    );
  const { data: ansatt, isLoading: isLoadingAnsatt } = useHentAnsatt();
  const { data: enheter, isLoading: isLoadingEnheter } = useAlleEnheter();

  const redigeringsModus = !!avtale;

  useEffect(() => {
    Modal.setAppElement("#root");
  }, []);

  const clickCancel = () => {
    onClose();
    handleCancel?.();
  };

  return (
    <>
      <Modal
        shouldCloseOnOverlayClick={false}
        closeButton
        open={modalOpen}
        onClose={clickCancel}
        className={styles.overstyrte_styles_fra_ds_modal}
        aria-label="modal"
      >
        <Modal.Content>
          <Heading size="medium" level="2" data-testid="avtale_modal_header">
            {redigeringsModus ? "Rediger avtale" : "Registrer ny avtale"}
          </Heading>
          {isLoadingAnsatt || isLoadingTiltakstyper || isLoadingEnheter ? (
            <Laster />
          ) : null}
          {!tiltakstyper?.data || !ansatt || !enheter ? null : (
            <OpprettAvtaleContainer
              onAvbryt={clickCancel}
              tiltakstyper={tiltakstyper.data}
              ansatt={ansatt}
              enheter={enheter}
              avtale={avtale}
            />
          )}
        </Modal.Content>
      </Modal>
    </>
  );
};

export default OpprettAvtaleModal;
