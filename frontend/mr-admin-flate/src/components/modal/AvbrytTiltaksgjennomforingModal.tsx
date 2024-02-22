import { useAvbrytTiltaksgjennomforing } from "../../api/tiltaksgjennomforing/useAvbrytTiltaksgjennomforing";
import styles from "./Modal.module.scss";
import { resolveErrorMessage } from "../../api/errors";
import { XMarkOctagonFillIcon } from "@navikt/aksel-icons";
import classNames from "classnames";
import { Heading, BodyShort, Button, Modal } from "@navikt/ds-react";
import { Tiltaksgjennomforing } from "mulighetsrommet-api-client";
import { RefObject, useEffect } from "react";
import { useNavigate } from "react-router-dom";

interface Props {
  modalRef: RefObject<HTMLDialogElement>;
  tiltaksgjennomforing: Tiltaksgjennomforing;
}

export const AvbrytTiltaksgjennomforingModal = ({ modalRef, tiltaksgjennomforing }: Props) => {
  const mutation = useAvbrytTiltaksgjennomforing();
  const navigate = useNavigate();

  const onClose = () => {
    mutation.reset();
    modalRef.current?.close();
  };

  useEffect(() => {
    if (mutation.isSuccess) {
      navigate(`/tiltaksgjennomforinger/${tiltaksgjennomforing?.id}`);
    }
  }, [mutation]);

  const handleAvbryt = () => {
    if (tiltaksgjennomforing?.id) {
      mutation.mutate(tiltaksgjennomforing?.id);
    }
  };

  return (
    <Modal ref={modalRef} onClose={onClose} closeOnBackdropClick aria-label="modal">
      <Modal.Header closeButton={false}>
        <div className={styles.heading}>
          <XMarkOctagonFillIcon className={classNames(styles.icon_warning, styles.icon)} />
          <Heading size="medium">
            {mutation.isError
              ? `Kan ikke avbryte «${tiltaksgjennomforing?.navn}»`
              : `Ønsker du å avbryte «${tiltaksgjennomforing?.navn}»?`}
          </Heading>
        </div>
      </Modal.Header>
      <Modal.Body>
        <BodyShort>
          {mutation?.isError
            ? resolveErrorMessage(mutation.error)
            : `Du kan ikke avbryte en gjennomføring som har deltakere
            tilknyttet seg.`}
        </BodyShort>
      </Modal.Body>
      <Modal.Footer>
        <div className={styles.knapperad}>
          <Button variant="secondary" type="button" onClick={onClose}>
            Lukk
          </Button>
          {!mutation?.isError && (
            <Button variant="danger" onClick={handleAvbryt}>
              Avbryt gjennomføring
            </Button>
          )}
        </div>
      </Modal.Footer>
    </Modal>
  );
};
