import { XMarkOctagonFillIcon } from "@navikt/aksel-icons";
import { BodyShort, Button, Heading, Modal } from "@navikt/ds-react";
import classNames from "classnames";
import { Avtale } from "mulighetsrommet-api-client";
import { RefObject, useEffect } from "react";
import { useAvbrytAvtale } from "@/api/avtaler/useAvbrytAvtale";
import styles from "./Modal.module.scss";
import { resolveErrorMessage } from "@/api/errors";
import { useNavigate } from "react-router-dom";
import { HarSkrivetilgang } from "../authActions/HarSkrivetilgang";

interface Props {
  modalRef: RefObject<HTMLDialogElement>;
  avtale: Avtale;
}

export const AvbrytAvtaleModal = ({ modalRef, avtale }: Props) => {
  const mutation = useAvbrytAvtale();
  const navigate = useNavigate();

  const onClose = () => {
    mutation.reset();
    modalRef.current?.close();
  };

  useEffect(() => {
    if (mutation.isSuccess) {
      navigate(`/avtaler/${avtale.id}`);
    }
  }, [mutation]);

  const handleAvbrytAvtale = () => {
    if (avtale?.id) {
      mutation.mutate(avtale?.id);
    }
  };

  return (
    <Modal ref={modalRef} onClose={onClose} closeOnBackdropClick aria-label="modal">
      <Modal.Header closeButton={false}>
        <div className={styles.heading}>
          <XMarkOctagonFillIcon className={classNames(styles.icon_warning, styles.icon)} />
          <Heading size="medium">
            {mutation.isError
              ? `Kan ikke avbryte «${avtale?.navn}»`
              : `Ønsker du å avbryte «${avtale?.navn}»?`}
          </Heading>
        </div>
      </Modal.Header>
      <Modal.Body>
        <BodyShort>
          {mutation?.isError
            ? resolveErrorMessage(mutation.error)
            : `Du kan ikke avbryte en avtale som har tiltaksgjennomføringer
            tilknyttet seg.`}
        </BodyShort>
      </Modal.Body>
      <Modal.Footer>
        <div className={styles.knapperad}>
          <Button variant="secondary" onClick={onClose}>
            Lukk
          </Button>
          <HarSkrivetilgang ressurs="Avtale">
            {!mutation?.isError && (
              <Button variant="danger" onClick={handleAvbrytAvtale}>
                Avbryt avtale
              </Button>
            )}
          </HarSkrivetilgang>
        </div>
      </Modal.Footer>
    </Modal>
  );
};
