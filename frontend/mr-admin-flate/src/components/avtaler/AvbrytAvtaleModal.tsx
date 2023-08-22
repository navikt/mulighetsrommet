import { XMarkOctagonFillIcon } from "@navikt/aksel-icons";
import { Button, Heading, Modal } from "@navikt/ds-react";
import classNames from "classnames";
import { ApiError, Avtale, Opphav } from "mulighetsrommet-api-client";
import { useEffect } from "react";
import { useAvbrytAvtale } from "../../api/avtaler/useAvbrytAvtale";
import { Lenkeknapp } from "../lenkeknapp/Lenkeknapp";
import styles from "../modal/Modal.module.scss";

interface Props {
  modalOpen: boolean;
  onClose: () => void;
  handleForm?: () => void;
  avtale: Avtale;
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

  const handleRedigerAvtale = () => {
    clickCancel();
    mutation.reset();
  };

  function headerInnhold(avtale?: Avtale) {
    return (
      <div className={styles.heading}>
        <XMarkOctagonFillIcon className={styles.warningicon} />
        {avtaleFraArena ? (
          <span>Avtalen kan ikke avbrytes</span>
        ) : mutation.isError ? (
          <span>Kan ikke avbryte «{avtale?.navn}»</span>
        ) : (
          <span>Ønsker du å avbryte «{avtale?.navn}»?</span>
        )}
      </div>
    );
  }

  function modalInnhold(avtale: Avtale) {
    return (
      <>
        {avtaleFraArena ? (
          <p>
            Avtalen {avtale.navn} kommer fra Arena og kan ikke avbrytes her
          </p>
        ) : mutation?.isError ? (
          <p>{(mutation.error as ApiError).body}</p>
        ) : (
          <p>Du kan ikke angre denne handlingen</p>
        )}
        <div className={styles.knapperad}>
          <Button variant="secondary" onClick={clickCancel}>
            Avbryt handling
          </Button>
          {avtaleFraArena ? null : mutation?.isError ? (
            <Lenkeknapp
              to={`/avtaler/${avtale.id}/skjema`}
              handleClick={handleRedigerAvtale}
              lenketekst={"Rediger avtale"}
              variant="primary"
            />
          ) : (
            <Button variant="danger" onClick={handleAvbrytAvtale}>
              Avbryt avtale
            </Button>
          )}
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
