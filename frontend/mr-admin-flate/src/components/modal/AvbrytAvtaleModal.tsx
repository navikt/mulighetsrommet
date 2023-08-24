import { XMarkOctagonFillIcon } from "@navikt/aksel-icons";
import { BodyShort, Button, Heading, Modal } from "@navikt/ds-react";
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
  function headerInnhold() {
    return (
      <div className={styles.heading}>
        <XMarkOctagonFillIcon
          className={classNames(styles.icon_warning, styles.icon)}
        />
        <Heading size="medium">
          {avtaleFraArena
            ? "Avtalen kan ikke avbrytes."
            : mutation.isError
            ? `Kan ikke avbryte «${avtale?.navn}»`
            : `Ønsker du å avbryte «${avtale?.navn}»?`}
        </Heading>
      </div>
    );
  }

  function modalInnhold() {
    return (
      <BodyShort>
        {error
          ? avtaleFraArena
            ? `Avtalen ${avtale?.navn} kommer fra Arena og kan ikke avbrytes her.`
            : mutation?.isError
            ? (mutation.error as ApiError).body
            : "Du kan ikke angre denne handlingen."
          : null}
      </BodyShort>
    );
  }

  function footerInnhold() {
    return (
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
    );
  }
  return (
    <Modal open={modalOpen} onClose={onClose} aria-label="modal">
      <Modal.Header closeButton={false}>{headerInnhold()}</Modal.Header>
      <Modal.Body>{modalInnhold()}</Modal.Body>
      <Modal.Footer>{footerInnhold()}</Modal.Footer>
    </Modal>
  );
};

export default AvbrytAvtaleModal;
