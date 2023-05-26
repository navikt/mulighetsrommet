import { Heading, Modal, Search } from "@navikt/ds-react";
import React from "react";
import styles from "./LeggTilGjennomforingModal.module.scss";
import { useAtom } from "jotai";
import { tiltaksgjennomforingTilAvtaleFilter } from "../../api/atoms";
import { Tiltaksgjennomforingsliste } from "../tiltaksgjennomforinger/Tiltaksgjennomforingsliste";

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
  const clickCancel = () => {
    // setError(null);
    onClose();
    handleCancel?.();
  };

  // const [error, setError] = useState<React.ReactNode | null>(null);
  const [filter, setFilter] = useAtom(tiltaksgjennomforingTilAvtaleFilter);

  // useEffect(() => {
  //   Modal.setAppElement("#root");
  // });

  return (
    <>
      {/*{!error && (*/}
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
            onChange={(e) => setFilter({ ...filter, search: e })}
          />
          <Tiltaksgjennomforingsliste />
        </Modal.Content>
      </Modal>
      {/*)}*/}
      {/*{error && (*/}
      {/*  <StatusModal*/}
      {/*    modalOpen={modalOpen}*/}
      {/*    ikonVariant="error"*/}
      {/*    heading="Kunne ikke legge til gjennomføring"*/}
      {/*    text={error}*/}
      {/*    onClose={clickCancel}*/}
      {/*    primaryButtonOnClick={() => setError(null)}*/}
      {/*    primaryButtonText="Prøv igjen"*/}
      {/*    secondaryButtonOnClick={clickCancel}*/}
      {/*    secondaryButtonText="Avbryt"*/}
      {/*  />*/}
      {/*)}*/}
    </>
  );
};
