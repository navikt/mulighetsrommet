import { Button, Heading, Modal } from "@navikt/ds-react";
import {
  ApiError,
  Avtale,
  Opphav,
  Tiltaksgjennomforing,
} from "mulighetsrommet-api-client";
import { useEffect } from "react";
import classNames from "classnames";
import { useNavigate } from "react-router-dom";
import { XMarkOctagonFillIcon } from "@navikt/aksel-icons";
import styles from "../modal/Modal.module.scss";
import { Lenkeknapp } from "../lenkeknapp/Lenkeknapp";
import { UseMutationResult } from "@tanstack/react-query";

interface Props {
  modalOpen: boolean;
  handleCancel: () => void;
  data: Tiltaksgjennomforing | Avtale;
  mutation: UseMutationResult<string, unknown, string>;
  dataType: "tiltaksgjennomforing" | "avtale";
}

const SlettAvtaleGjennomforingModal = ({
  modalOpen,
  handleCancel,
  data,
  mutation,
  dataType,
}: Props) => {
  const navigate = useNavigate();

  const fraArena = data?.opphav === Opphav.ARENA;

  useEffect(() => {
    if (mutation.isSuccess) {
      navigate(`/${dataType}er`);
      return;
    }
  }, [mutation]);

  const handleDelete = () => {
    mutation.mutate(data.id);
  };
  const tekster = {
    tiltaksgjennomforing: { navnPlural: "Gjennomføringen" },
    avtale: { navnPlural: "Avtalen" },
  };

  function headerInnhold() {
    return (
      <div className={styles.heading}>
        <XMarkOctagonFillIcon className={styles.warningicon} />

        {fraArena ? (
          <span>{tekster[dataType].navnPlural} kan ikke slettes</span>
        ) : mutation.isError ? (
          <span>Kan ikke slette «{data.navn}»</span>
        ) : (
          <span>Ønsker du å slette «{data.navn}»?</span>
        )}
      </div>
    );
  }

  function modalInnhold() {
    return (
      <>
        {fraArena ? (
          <p>
            {tekster[dataType].navnPlural} «{data.navn}» kommer fra Arena og kan
            ikke slettes her
          </p>
        ) : mutation?.isError ? (
          <p>{(mutation.error as ApiError).body}</p>
        ) : (
          <p>Du kan ikke angre denne handlingen</p>
        )}
        <div className={styles.knapperad}>
          {fraArena ? null : mutation?.isError ? (
            <Lenkeknapp
              to={`/tiltaksgjennomforinger/skjema?tiltaksgjennomforingId=${data?.id}`}
              lenketekst="Rediger tiltaksgjennomføring"
              variant="primary"
            />
          ) : (
            <Button variant="danger" onClick={handleDelete}>
              Slett{" "}
              {dataType === "tiltaksgjennomforing"
                ? "tiltaksgjennomføring"
                : "avtale"}
            </Button>
          )}
          <Button variant="secondary-neutral" onClick={handleCancel}>
            Avbryt
          </Button>
        </div>
      </>
    );
  }

  return (
    <Modal
      shouldCloseOnOverlayClick={false}
      closeButton
      open={modalOpen}
      onClose={handleCancel}
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
  );
};

export default SlettAvtaleGjennomforingModal;
