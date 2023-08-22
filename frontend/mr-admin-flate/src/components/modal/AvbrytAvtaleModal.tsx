import { XMarkOctagonFillIcon } from "@navikt/aksel-icons";
import { BodyShort, Button, Heading, Modal } from "@navikt/ds-react";
import classNames from "classnames";
import { ApiError, Avtale, Opphav } from "mulighetsrommet-api-client";
import { useEffect } from "react";
import { useAvbrytAvtale } from "../../api/avtaler/useAvbrytAvtale";
import { Lenkeknapp } from "../lenkeknapp/Lenkeknapp";
import styles from "./Modal.module.scss";

interface Props {
  modalOpen: boolean;
  onClose: () => void;
  handleForm?: () => void;
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

  const handleRedigerAvtale = () => {
    clickCancel();
    mutation.reset();
  };

  function headerInnhold() {
    return (
      <div className={styles.heading}>
        <XMarkOctagonFillIcon
          className={classNames(styles.icon_warning, styles.icon)}
        />
        {avtaleFraArena ? (
          <Heading size="medium">Avtalen kan ikke avbrytes</Heading>
        ) : mutation.isError ? (
          <Heading size="medium">Kan ikke avbryte «{avtale?.navn}»</Heading>
        ) : (
          <Heading size="medium">Ønsker du å avbryte «{avtale?.navn}»?</Heading>
        )}
      </div>
    );
  }

  function modalInnhold() {
    return (
      <BodyShort>
        {avtaleFraArena
          ? `Avtalen ${avtale?.navn} kommer fra Arena og kan ikke avbrytes her.`
          : mutation?.isError
          ? (mutation.error as ApiError).body
          : "Du kan ikke angre denne handlingen."}
      </BodyShort>
    );
  }

  function footerInnhold() {
    return (
      <div className={styles.knapperad}>
        <Button
          variant={avtaleFraArena ? "primary" : "secondary"}
          onClick={clickCancel}
        >
          Avbryt handling
        </Button>
        {avtaleFraArena ? null : mutation?.isError ? (
          <Lenkeknapp
            to={`/avtaler/skjema?avtaleId=${avtale?.id}`}
            handleClick={handleRedigerAvtale}
            lenketekst="Rediger avtale"
            variant="primary"
          />
        ) : (
          <Button variant="danger" onClick={handleAvbrytAvtale}>
            Avbryt avtale
          </Button>
        )}
      </div>
    );
  }

  return (
    <Modal open={modalOpen} onClose={clickCancel} aria-label="modal">
      <Modal.Header closeButton={false}>{headerInnhold()}</Modal.Header>
      <Modal.Body>{modalInnhold()}</Modal.Body>
      <Modal.Footer>{footerInnhold()}</Modal.Footer>
    </Modal>
  );
};

export default AvbrytAvtaleModal;
