import { BodyShort, Button, Heading, Modal } from "@navikt/ds-react";
import { ApiError, Avtale, Opphav, Tiltaksgjennomforing } from "mulighetsrommet-api-client";
import { useEffect } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { XMarkOctagonFillIcon } from "@navikt/aksel-icons";
import styles from "../modal/Modal.module.scss";
import { Lenkeknapp } from "../lenkeknapp/Lenkeknapp";
import { UseMutationResult } from "@tanstack/react-query";
import classNames from "classnames";
import { useGetAdminTiltaksgjennomforingsIdFraUrl } from "../../hooks/useGetAdminTiltaksgjennomforingsIdFraUrl";
import { useGetAvtaleIdFromUrl } from "../../hooks/useGetAvtaleIdFromUrl";
import { parentPath } from "../navigering/Tilbakelenke";
import { resolveErrorMessage } from "../../api/errors";

interface Props {
  modalOpen: boolean;
  handleCancel: () => void;
  data: Tiltaksgjennomforing | Avtale;
  mutation: UseMutationResult<string, ApiError, string>;
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
  const { pathname } = useLocation();
  const avtaleId = useGetAvtaleIdFromUrl();
  const tiltaksgjennomforingId = useGetAdminTiltaksgjennomforingsIdFraUrl();

  const fraArena = data?.opphav === Opphav.ARENA;

  useEffect(() => {
    if (mutation.isSuccess) {
      navigate(parentPath(pathname, avtaleId, tiltaksgjennomforingId));
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
        <XMarkOctagonFillIcon className={classNames(styles.icon_warning, styles.icon)} />

        <Heading size="medium">
          {fraArena
            ? `${tekster[dataType].navnPlural} kan ikke slettes`
            : mutation.isError
            ? `Kan ikke slette «${data.navn}»`
            : `Ønsker du å slette «${data.navn}»?`}
        </Heading>
      </div>
    );
  }

  function modalInnhold() {
    return (
      <BodyShort>
        {fraArena
          ? `${tekster[dataType].navnPlural} «${data.navn}» kommer fra Arena og kan
            ikke slettes her`
          : mutation?.isError
          ? resolveErrorMessage(mutation.error)
          : "Du kan ikke angre denne handlingen."}
      </BodyShort>
    );
  }

  function footerInnhold() {
    return (
      <div className={styles.knapperad}>
        <Button variant="secondary" onClick={handleCancel}>
          Avbryt
        </Button>
        {fraArena ? null : mutation?.isError ? (
          <Lenkeknapp
            to={`/tiltaksgjennomforinger/skjema?tiltaksgjennomforingId=${data?.id}`}
            lenketekst="Rediger tiltaksgjennomføring"
            variant="primary"
          />
        ) : (
          <Button variant="danger" onClick={handleDelete}>
            Slett {dataType === "tiltaksgjennomforing" ? "tiltaksgjennomføring" : "avtale"}
          </Button>
        )}
      </div>
    );
  }

  return (
    <Modal open={modalOpen} onClose={handleCancel} aria-label="modal">
      <Modal.Header closeButton={false}>{headerInnhold()}</Modal.Header>
      <Modal.Body>{modalInnhold()}</Modal.Body>
      <Modal.Footer>{footerInnhold()}</Modal.Footer>
    </Modal>
  );
};

export default SlettAvtaleGjennomforingModal;
