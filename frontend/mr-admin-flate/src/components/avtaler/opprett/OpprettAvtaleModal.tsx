import { Heading, Modal } from "@navikt/ds-react";
import { Avtale } from "mulighetsrommet-api-client";
import { StatusModal } from "mulighetsrommet-veileder-flate/src/components/modal/delemodal/StatusModal";
import { useEffect, useState } from "react";
import { useHentAnsatt } from "../../../api/ansatt/useHentAnsatt";
import { useAlleEnheter } from "../../../api/enhet/useAlleEnheter";
import { useAlleTiltakstyper } from "../../../api/tiltakstyper/useAlleTiltakstyper";
import { useNavigerTilAvtale } from "../../../hooks/useNavigerTilAvtale";
import { Laster } from "../../laster/Laster";
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
  const { navigerTilAvtale } = useNavigerTilAvtale();
  const { data: tiltakstyper, isLoading: isLoadingTiltakstyper } =
    useAlleTiltakstyper();
  const { data: ansatt, isLoading: isLoadingAnsatt } = useHentAnsatt();
  const { data: enheter, isLoading: isLoadingEnheter } = useAlleEnheter();

  const redigeringsModus = !!avtale;

  useEffect(() => {
    Modal.setAppElement("#root");
  }, []);

  const clickCancel = () => {
    setError(null);
    setResult(null);
    onClose();
    handleCancel!();
  };

  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<string | null>(null);

  return (
    <>
      {!error && !result && (
        <Modal
          shouldCloseOnOverlayClick={false}
          closeButton
          open={modalOpen}
          onClose={clickCancel}
          className={styles.overstyrte_styles_fra_ds_modal}
          aria-label="modal"
        >
          <Modal.Content>
            <Heading size="small" level="2" data-testid="avtale_modal_header">
              {redigeringsModus ? "Rediger avtale" : "Registrer ny avtale"}
            </Heading>
            {isLoadingAnsatt || isLoadingTiltakstyper || isLoadingEnheter ? (
              <Laster />
            ) : null}
            {!tiltakstyper?.data || !ansatt || !enheter ? null : (
              <OpprettAvtaleContainer
                tiltakstyper={tiltakstyper?.data?.filter((tiltakstype) =>
                  ["VASV", "ARBFORB"].includes(tiltakstype.arenaKode)
                )}
                ansatt={ansatt}
                setResult={setResult}
                enheter={enheter}
                avtale={avtale}
              />
            )}
          </Modal.Content>
        </Modal>
      )}
      {result && (
        <StatusModal
          modalOpen={modalOpen}
          onClose={clickCancel}
          ikonVariant="success"
          heading="Avtalen er opprettet."
          text="Avtalen ble opprettet."
          primaryButtonText="Gå til avtalen"
          primaryButtonOnClick={() => navigerTilAvtale(result)}
          secondaryButtonText="Lukk"
          secondaryButtonOnClick={clickCancel}
        />
      )}
    </>
  );
};

export default OpprettAvtaleModal;
