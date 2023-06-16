import { Button, Heading, Modal } from "@navikt/ds-react";
import {
  ApiError,
  Opphav,
  Tiltaksgjennomforing,
} from "mulighetsrommet-api-client";
import { useEffect } from "react";
import classNames from "classnames";
import { useNavigate } from "react-router-dom";
import styles from "../avtaler/Modal.module.scss";
import { VarselIkon } from "../avtaler/SlettAvtaleModal";
import { useDeleteTiltaksgjennomforing } from "../../api/tiltaksgjennomforing/useDeleteTiltaksgjennomforing";

interface SlettTiltaksgjennomforingModalprops {
  modalOpen: boolean;
  onClose: () => void;
  handleForm?: () => void;
  handleCancel?: () => void;
  handleRediger?: () => void;
  shouldCloseOnOverlayClick?: boolean;
  tiltaksgjennomforing: Tiltaksgjennomforing;
}

const SlettTiltaksgjennomforingModal = ({
  modalOpen,
  onClose,
  handleCancel,
  handleRediger,
  tiltaksgjennomforing,
}: SlettTiltaksgjennomforingModalprops) => {
  const mutation = useDeleteTiltaksgjennomforing();
  const navigate = useNavigate();
  useEffect(() => {
    Modal.setAppElement("#root");
  }, []);

  useEffect(() => {
    if (mutation.isSuccess) {
      navigate("/tiltaksgjennomforinger");
      return;
    }
  }, [mutation]);

  const clickCancel = () => {
    onClose();
    handleCancel?.();
  };

  const handleDelete = () => {
    mutation.mutate(tiltaksgjennomforing.id);
  };

  const handleRedigerGjennomforing = () => {
    clickCancel();
    mutation.reset();
    handleRediger?.();
  };

  function headerInnhold(tiltaksgjennomforing: Tiltaksgjennomforing) {
    return (
      <div className={styles.heading}>
        <VarselIkon />
        {tiltaksgjennomforing.opphav === Opphav.ARENA ? (
          <span>Gjennomføringen kan ikke slettes</span>
        ) : mutation.isError ? (
          <span>Kan ikke slette «{tiltaksgjennomforing.navn}»</span>
        ) : (
          <span>Ønsker du å slette «{tiltaksgjennomforing.navn}»?</span>
        )}
      </div>
    );
  }

  function modalInnhold(tiltaksgjennomforing: Tiltaksgjennomforing) {
    return (
      <>
        {tiltaksgjennomforing.opphav === Opphav.ARENA ? (
          <p>
            Gjennomføringen «{tiltaksgjennomforing.navn}» kommer fra Arena og
            kan ikke slettes her
          </p>
        ) : mutation?.isError ? (
          <p>{(mutation.error as ApiError).body}</p>
        ) : (
          <>
            <p>
              Er du sikker på at du ønsker å slette gjennomføringen «
              {tiltaksgjennomforing.navn}»?
            </p>
            <p>Du kan ikke angre denne handlingen</p>
          </>
        )}
        <div className={styles.knapperad}>
          {tiltaksgjennomforing.opphav ===
          Opphav.ARENA ? null : mutation?.isError ? (
            <Button variant="primary" onClick={handleRedigerGjennomforing}>
              Rediger gjennomføring
            </Button>
          ) : (
            <Button variant="danger" onClick={handleDelete}>
              Slett gjennomføring
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
            data-testid="slett_gjennomforing_modal_header"
          >
            {headerInnhold(tiltaksgjennomforing)}
          </Heading>
          {modalInnhold(tiltaksgjennomforing)}
        </Modal.Content>
      </Modal>
    </>
  );
};

export default SlettTiltaksgjennomforingModal;
