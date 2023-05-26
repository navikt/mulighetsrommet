import { Heading, Modal, Search } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { tiltaksgjennomforingTilAvtaleFilter } from "../../api/atoms";
import { Tiltaksgjennomforingsliste } from "../tiltaksgjennomforinger/Tiltaksgjennomforingsliste";
import styles from "./LeggTilGjennomforingModal.module.scss";

interface ModalProps {
  modalOpen: boolean;
  onClose: () => void;
  handleForm?: () => void;
  handleCancel?: () => void;
  shouldCloseOnOverlayClick?: boolean;
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
    <>
      <Modal
        shouldCloseOnOverlayClick={false}
        closeButton
        open={modalOpen}
        onClose={clickCancel}
        className={styles.modal_container}
        aria-label="modal"
      >
        <Modal.Content className={styles.modal_content}>
          <Heading size="medium" level="2">
            Legg til ny gjennomføring til avtalen
          </Heading>
          <Search
            label="Søk på navn eller tiltaksnummer"
            variant="simple"
            hideLabel={false}
            autoFocus
            onChange={(search) =>
              setFilter({ ...filter, search: search.trim() })
            }
          />
          <Tiltaksgjennomforingsliste />
        </Modal.Content>
      </Modal>
    </>
  );
};
