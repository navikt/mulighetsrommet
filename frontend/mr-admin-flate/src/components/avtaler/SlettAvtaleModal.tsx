import { Button, Heading, Modal } from "@navikt/ds-react";
import { ApiError, Avtale, Opphav } from "mulighetsrommet-api-client";
import { useEffect, useState } from "react";
import styles from "./Modal.module.scss";
import classNames from "classnames";
import { useDeleteAvtale } from "../../api/avtaler/useDeleteAvtale";
import { useNavigate } from "react-router-dom";

interface SlettAvtaleModalprops {
  modalOpen: boolean;
  onClose: () => void;
  handleForm?: () => void;
  handleCancel?: () => void;
  handleRediger?: () => void;
  shouldCloseOnOverlayClick?: boolean;
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
  useEffect(() => {
    Modal.setAppElement("#root");
  }, []);

  useEffect(() => {
    if (mutation.data?.statusCode === 200) {
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
        <VarselIkon />
        {avtale?.opphav === Opphav.ARENA ? (
          <span>Avtalen kan ikke slettes</span>
        ) : mutation.isError ? (
          <span>Kan ikke slette {avtale?.navn}</span>
        ) : (
          <span>Ønsker du å slette {avtale?.navn}?</span>
        )}
      </div>
    );
  }

  function modalInnhold(avtale?: Avtale) {
    return (
      <>
        {avtale?.opphav === Opphav.ARENA ? (
          <p>Avtalen {avtale?.navn} kommer fra Arena og kan ikke slettes her</p>
        ) : mutation?.isError ? (
          <p>{(mutation.error as ApiError).body}</p>
        ) : (
          <>
            <p>Er du sikker på at du ønsker å slette avtalen {avtale?.navn}?</p>
            <p>Du kan ikke angre denne handlingen</p>
          </>
        )}
        <div className={styles.knapperad}>
          {avtale?.opphav === Opphav.ARENA ? null : mutation?.isError ? (
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
          styles.text_center
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

function VarselIkon() {
  return (
    <svg
      width="32"
      height="32"
      viewBox="0 0 32 32"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path
        fillRule="evenodd"
        clipRule="evenodd"
        d="M3.14138e-05 15.968C0.016727 7.16242 7.19306 0 15.9986 0C24.8529 0.0166956 32.0167 7.20833 32 16.0292C31.9819 24.8361 24.8042 31.9999 16 31.9999H15.9694C11.6953 31.9916 7.68002 30.3192 4.66367 27.2918C1.64733 24.2629 -0.0083164 20.242 3.14138e-05 15.968ZM22.6666 11.2381L20.7619 9.33332L16 14.096L11.2381 9.33332L9.33332 11.2381L14.096 16L9.33332 20.7619L11.2381 22.6666L16 17.904L20.7619 22.6666L22.6666 20.7619L17.904 16L22.6666 11.2381Z"
        fill="#C30000"
      />
    </svg>
  );
}

export default SlettAvtaleModal;
