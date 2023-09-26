import { XMarkOctagonFillIcon } from "@navikt/aksel-icons";
import { BodyShort, Button, Heading, Modal } from "@navikt/ds-react";
import classNames from "classnames";
import { Avtale, Opphav } from "mulighetsrommet-api-client";
import { useEffect } from "react";
import { useAvbrytAvtale } from "../../api/avtaler/useAvbrytAvtale";
import { Lenkeknapp } from "../lenkeknapp/Lenkeknapp";
import styles from "./Modal.module.scss";
import { resolveErrorMessage } from "../../api/errors";

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
        <XMarkOctagonFillIcon className={classNames(styles.icon_warning, styles.icon)} />
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
        {avtaleFraArena
          ? `Avtalen ${avtale?.navn} kommer fra Arena og kan ikke avbrytes her.`
          : mutation?.isError
          ? resolveErrorMessage(mutation.error)
          : "Du kan ikke angre denne handlingen."}
      </BodyShort>
    );
  }

  function footerInnhold() {
    return (
      <div className={styles.knapperad}>
        <Button variant={avtaleFraArena ? "primary" : "secondary"} onClick={clickCancel}>
          Lukk
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
