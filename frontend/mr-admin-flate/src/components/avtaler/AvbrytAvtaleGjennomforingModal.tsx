import { XMarkOctagonFillIcon } from "@navikt/aksel-icons";
import { BodyShort, Button, Heading, Modal } from "@navikt/ds-react";
import classNames from "classnames";
import {
  ApiError,
  Avtale,
  Opphav,
  Tiltaksgjennomforing,
} from "mulighetsrommet-api-client";
import { useEffect } from "react";
import styles from "../modal/Modal.module.scss";

interface Props {
  modalOpen: boolean;
  onClose: () => void;
  data: Avtale | Tiltaksgjennomforing;
  mutation: any;
  type: "avtale" | "gjennomforing";
}

const AvbrytAvtaleGjennomforingModal = ({
  modalOpen,
  onClose,
  data,
  mutation,
  type,
}: Props) => {
  const dataFraArena = data?.opphav === Opphav.ARENA;
  const erAvtale = type === "avtale";

  useEffect(() => {
    if (mutation.isSuccess) {
      onClose();
      mutation.reset();
      return;
    }
  }, [mutation]);

  const handleAvbryt = () => {
    if (data?.id) {
      mutation.mutate(data?.id);
    }
  };

  function headerInnhold() {
    return (
      <div className={styles.heading}>
        <XMarkOctagonFillIcon className={styles.warningicon} />
        {dataFraArena
          ? `${
              erAvtale ? "Avtalen" : "Tiltaksgjennomføringen"
            } kan ikke avbrytes`
          : mutation.isError
          ? `Kan ikke avbryte «${data?.navn}»`
          : `Ønsker du å avbryte «${data?.navn}»?`}
      </div>
    );
  }

  function modalInnhold() {
    return (
      <>
        <div className={styles.modal_innhold}>
          {dataFraArena ? (
            <BodyShort>
              {erAvtale ? "Avtalen" : "Tiltaksgjennomføringen"} {data?.navn}{" "}
              kommer fra Arena og kan ikke avbrytes her.
            </BodyShort>
          ) : mutation?.isError ? (
            <BodyShort>{(mutation.error as ApiError).body}</BodyShort>
          ) : (
            <BodyShort>Du kan ikke angre denne handlingen.</BodyShort>
          )}
        </div>
        <div className={styles.knapperad}>
          {mutation?.isError ? (
            <Button variant="secondary" onClick={onClose}>
              Lukk
            </Button>
          ) : (
            <Button variant="secondary" onClick={onClose}>
              Avbryt handling
            </Button>
          )}
          {!dataFraArena && !mutation?.isError ? (
            <Button variant="danger" onClick={handleAvbryt}>
              Avbryt {erAvtale ? "avtale" : "tiltaksgjennomføring"}
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
            {headerInnhold()}
          </Heading>
          {modalInnhold()}
        </Modal.Content>
      </Modal>
    </>
  );
};

export default AvbrytAvtaleGjennomforingModal;
