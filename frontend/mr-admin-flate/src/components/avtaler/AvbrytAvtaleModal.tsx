import { XMarkOctagonFillIcon } from "@navikt/aksel-icons";
import { BodyShort, Button, Heading, Modal } from "@navikt/ds-react";
import classNames from "classnames";
import { ApiError, Avtale, Opphav } from "mulighetsrommet-api-client";
import { useEffect } from "react";
import { useAvbrytAvtale } from "../../api/avtaler/useAvbrytAvtale";
import styles from "../modal/Modal.module.scss";

interface Props {
  modalOpen: boolean;
  onClose: () => void;
  avtale?: Avtale;
}

const AvbrytAvtaleModal = ({ modalOpen, onClose, avtale }: Props) => {
  const mutation = useAvbrytAvtale();
  const avtaleFraArena = avtale?.opphav === Opphav.ARENA;

  useEffect(() => {
    if (mutation.isSuccess) {
      clickCancel();
      mutation.reset();
      return;
    }
  }, [mutation]);

  const clickCancel = () => {
    onClose();
  };

  const handleAvbrytAvtale = () => {
    if (avtale?.id) {
      mutation.mutate(avtale?.id);
    }
  };

  function headerInnhold(avtale?: Avtale) {
    return (
      <div className={styles.heading}>
        <XMarkOctagonFillIcon className={styles.warningicon} />
        {avtaleFraArena
          ? "Avtalen kan ikke avbrytes"
          : mutation.isError
          ? `Kan ikke avbryte «${avtale?.navn}»`
          : `Ønsker du å avbryte «${avtale?.navn}»?`}
      </div>
    );
  }

  function modalInnhold(avtale?: Avtale) {
    return (
      <>
        <div className={styles.modal_innhold}>
          {avtaleFraArena ? (
            <BodyShort>
              Avtalen {avtale?.navn} kommer fra Arena og kan ikke avbrytes her.
            </BodyShort>
          ) : mutation?.isError ? (
            <BodyShort>{(mutation.error as ApiError).body}</BodyShort>
          ) : (
            <BodyShort>Du kan ikke angre denne handlingen.</BodyShort>
          )}
        </div>
        <div className={styles.knapperad}>
          {mutation?.isError ? (
            <Button variant="secondary" onClick={clickCancel}>
              Lukk
            </Button>
          ) : (
            <Button variant="secondary" onClick={clickCancel}>
              Avbryt handling
            </Button>
          )}
          {!avtaleFraArena && !mutation?.isError ? (
            <Button variant="danger" onClick={handleAvbrytAvtale}>
              Avbryt avtale
            </Button>
          ) : null}
        </div>
      </>
    );
  }

  return (
    <>
      <Modal
        shouldCloseOnOverlayClick={false}
        closeButton
        open={modalOpen}
        onClose={clickCancel}
        className={classNames(
          styles.overstyrte_styles_fra_ds_modal,
          styles.text_center,
        )}
        aria-label="modal"
      >
        <Modal.Content>
          <Heading size="medium" level="2">
            {headerInnhold(avtale)}
          </Heading>
          {modalInnhold(avtale)}
        </Modal.Content>
      </Modal>
    </>
  );
};

export default AvbrytAvtaleModal;
