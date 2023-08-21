import { Modal, Search } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { tiltaksgjennomforingTilAvtaleFilter } from "../../api/atoms";
import { Tiltaksgjennomforingsliste } from "../tiltaksgjennomforinger/Tiltaksgjennomforingsliste";
import styles from "./LeggTilGjennomforingModal.module.scss";

interface ModalProps {
  modalOpen: boolean;
  onClose: () => void;
  handleForm?: () => void;
  handleCancel?: () => void;
}

export const LeggTilGjennomforingModal = ({
  modalOpen,
  onClose,
  handleCancel,
}: ModalProps) => {
  const [filter, setFilter] = useAtom(tiltaksgjennomforingTilAvtaleFilter);

  const clickCancel = () => {
    setFilter({ search: "" });
    onClose();
    handleCancel?.();
  };

  return (
    <Modal
      open={modalOpen}
      onClose={clickCancel}
      className={styles.modal_container}
      aria-label="modal"
    >
      <Modal.Header closeButton>
        Legg til ny gjennomføring til avtalen
      </Modal.Header>
      <Modal.Body className={styles.modal_content}>
        <Search
          label="Søk på navn eller tiltaksnummer"
          variant="simple"
          hideLabel={false}
          autoFocus
          onChange={(search) => setFilter({ ...filter, search: search.trim() })}
        />
        <Tiltaksgjennomforingsliste />
      </Modal.Body>
    </Modal>
  );
};
