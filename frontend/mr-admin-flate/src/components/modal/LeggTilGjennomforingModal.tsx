import { Heading, Modal, Search } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { tiltaksgjennomforingTilAvtaleFilterAtom } from "../../api/atoms";
import { Tiltaksgjennomforingsliste } from "../tiltaksgjennomforinger/Tiltaksgjennomforingsliste";
import styles from "./LeggTilGjennomforingModal.module.scss";
import { useGetAvtaleIdFromUrlOrThrow } from "../../hooks/useGetAvtaleIdFromUrl";

interface ModalProps {
  modalOpen: boolean;
  onClose: () => void;
  handleForm?: () => void;
  handleCancel?: () => void;
}

export const LeggTilGjennomforingModal = ({ modalOpen, onClose, handleCancel }: ModalProps) => {
  const avtaleId = useGetAvtaleIdFromUrlOrThrow();
  const [filter, setFilter] = useAtom(tiltaksgjennomforingTilAvtaleFilterAtom);

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
      width="50rem"
    >
      <Modal.Header closeButton>
        <Heading size="medium">Legg til ny gjennomføring til avtalen</Heading>
      </Modal.Header>
      <Modal.Body className={styles.modal_content}>
        <Search
          label="Søk på navn eller tiltaksnummer"
          variant="simple"
          hideLabel={false}
          autoFocus
          onChange={(search) =>
            setFilter({
              ...filter,
              search: search.trim(),
            })
          }
        />
        <Tiltaksgjennomforingsliste avtaleId={avtaleId} search={filter.search} />
      </Modal.Body>
    </Modal>
  );
};
