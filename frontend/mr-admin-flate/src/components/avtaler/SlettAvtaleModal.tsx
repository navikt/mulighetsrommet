import { Button, Heading, Modal } from "@navikt/ds-react";
import { ApiError, Avtale, Opphav } from "mulighetsrommet-api-client";
import { useEffect } from "react";
import classNames from "classnames";
import { useNavigate } from "react-router-dom";
import { useDeleteAvtale } from "../../api/avtaler/useDeleteAvtale";
import styles from "../modal/Modal.module.scss";
import { XMarkOctagonFillIcon } from "@navikt/aksel-icons";

interface SlettAvtaleModalprops {
  modalOpen: boolean;
  onClose: () => void;
  handleForm?: () => void;
  handleCancel?: () => void;
  handleRediger?: () => void;
  avtale?: Avtale;
}

const SlettAvtaleModal = ({
  modalOpen,
  onClose,
  handleCancel,
  handleRediger,
  avtale,
}: SlettAvtaleModalprops) => {
  const mutation = useDeleteAvtale();
  const navigate = useNavigate();
  const avtaleFraArena = avtale?.opphav === Opphav.ARENA;

  useEffect(() => {
    Modal.setAppElement("#root");
  }, []);

  useEffect(() => {
    if (mutation.isSuccess) {
      navigate("/avtaler");
      return;
    }
  }, [mutation]);

  const clickCancel = () => {
    onClose();
    handleCancel?.();
  };

  const handleDelete = () => {
    if (avtale?.id) {
      mutation.mutate(avtale?.id);
    }
  };

  const handleRedigerAvtale = () => {
    clickCancel();
    mutation.reset();
    handleRediger?.();
  };

  function headerInnhold(avtale?: Avtale) {
    return (
      <div className={styles.heading}>
        <XMarkOctagonFillIcon className={styles.warningicon} />
        {avtaleFraArena ? (
          <span>Avtalen kan ikke slettes</span>
        ) : mutation.isError ? (
          <span>Kan ikke slette «{avtale?.navn}»</span>
        ) : (
          <span>Ønsker du å slette «{avtale?.navn}»?</span>
        )}
      </div>
    );
  }

  function modalInnhold(avtale?: Avtale) {
    return (
      <>
        {avtaleFraArena ? (
          <p>Avtalen {avtale?.navn} kommer fra Arena og kan ikke slettes her</p>
        ) : mutation?.isError ? (
          <p>{(mutation.error as ApiError).body}</p>
        ) : (
          <p>Du kan ikke angre denne handlingen</p>
        )}
        <div className={styles.knapperad}>
          {avtaleFraArena ? null : mutation?.isError ? (
            <Button variant="primary" onClick={handleRedigerAvtale}>
              Rediger avtale
            </Button>
          ) : (
            <Button variant="danger" onClick={handleDelete}>
              Slett avtale
            </Button>
          )}
          <Button variant="secondary-neutral" onClick={clickCancel}>
            Avbryt
          </Button>
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
          <Heading
            size="medium"
            level="2"
            data-testid="slett_avtale_modal_header"
          >
            {headerInnhold(avtale)}
          </Heading>
          {modalInnhold(avtale)}
        </Modal.Content>
      </Modal>
    </>
  );
};

export default SlettAvtaleModal;
