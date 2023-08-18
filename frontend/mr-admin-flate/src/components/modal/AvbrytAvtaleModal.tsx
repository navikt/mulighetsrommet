import { XMarkOctagonFillIcon } from "@navikt/aksel-icons";
import { Button, Heading, Modal } from "@navikt/ds-react";
import classNames from "classnames";
import { ApiError, Avtale, Opphav } from "mulighetsrommet-api-client";
import { useEffect } from "react";
import { useAvbrytAvtale } from "../../api/avtaler/useAvbrytAvtale";
import styles from "./Modal.module.scss";

interface Props {
  modalOpen: boolean;
  onClose: () => void;
  handleForm?: () => void;
  avtale?: Avtale;
  error: ApiError | null;
}

const AvbrytAvtaleModal = ({ modalOpen, onClose, avtale, error }: Props) => {
  const mutation = useAvbrytAvtale();
  const avtaleFraArena = avtale?.opphav === Opphav.ARENA;

  useEffect(() => {
    if (mutation.isSuccess) {
      onClose();
      mutation.reset();
      return;
    }
  }, [mutation]);

  const handleAvbrytAvtale = () => {
    if (avtale?.id) {
      mutation.mutate(avtale?.id);
    }
  };
  function headerInnhold(avtale?: Avtale) {
    return (
      <div className={styles.heading}>
        <XMarkOctagonFillIcon className={styles.warningicon} />
        {error ? (
          avtaleFraArena ? (
            <span>Avtalen kan ikke avbrytes</span>
          ) : mutation.isError ? (
            <span>Kan ikke avbryte «{avtale?.navn}»</span>
          ) : (
            <span>Ønsker du å avbryte «{avtale?.navn}»?</span>
          )
        ) : (
          <span>Fant ikke avtale id</span>
        )}
      </div>
    );
  }

  function modalInnhold(avtale?: Avtale) {
    return (
      <>
        {error ? (
          avtaleFraArena ? (
            <p>
              Avtalen {avtale?.navn} kommer fra Arena og kan ikke avbrytes her
            </p>
          ) : mutation?.isError ? (
            <p>{(mutation.error as ApiError).body}</p>
          ) : (
            <p>Du kan ikke angre denne handlingen</p>
          )
        ) : null}
        <div className={styles.knapperad}>
          <Button variant="secondary" onClick={onClose}>
            Avbryt handling
          </Button>
          {!avtaleFraArena && !mutation?.isError && error ? (
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
        onClose={onClose}
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
