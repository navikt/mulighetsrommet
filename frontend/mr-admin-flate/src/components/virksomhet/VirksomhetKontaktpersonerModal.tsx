import { PlusIcon } from "@navikt/aksel-icons";
import { BodyShort, Button, Label, Modal } from "@navikt/ds-react";
import { RefObject, useState } from "react";
import { useVirksomhetKontaktpersoner } from "../../api/virksomhet/useVirksomhetKontaktpersoner";
import styles from "./VirksomhetKontaktpersonerModal.module.scss";
import { Laster } from "../laster/Laster";
import { VirksomhetKontaktperson } from "mulighetsrommet-api-client";
import { VirksomhetKontaktpersonSkjema } from "./VirksomhetKontaktpersonSkjema";
import { useVirksomhet } from "../../api/virksomhet/useVirksomhet";

interface Props {
  orgnr: string;
  modalRef: RefObject<HTMLDialogElement>;
  onClose: () => void;
}

export function VirksomhetKontaktpersonerModal(props: Props) {
  const { orgnr, modalRef, onClose } = props;
  const { data: virksomhet, isLoading: isLoadingVirksomhet } = useVirksomhet(orgnr);
  const {
    data: kontaktpersoner,
    isLoading: isLoadingKontaktpersoner,
    refetch,
  } = useVirksomhetKontaktpersoner(orgnr);

  const [opprett, setOpprett] = useState<boolean>(false);
  const [redigerId, setRedigerId] = useState<string | undefined>(undefined);

  if (!virksomhet || !kontaktpersoner || isLoadingKontaktpersoner || isLoadingVirksomhet) {
    return <Laster />;
  }

  function reset() {
    setOpprett(false);
    setRedigerId(undefined);
    refetch();
  }

  return (
    <Modal
      ref={modalRef}
      className={styles.modal}
      onClose={() => {
        modalRef.current?.close();
        onClose();
      }}
    >
      <Modal.Header>
        <Label>{`Kontaktpersoner hos ${virksomhet.navn}`}</Label>
      </Modal.Header>
      <Modal.Body>
        <div className={styles.modal_body}>
          {kontaktpersoner
            .sort((a, b) => a.navn.localeCompare(b.navn))
            .map((person: VirksomhetKontaktperson) => (
              <div key={person.id} className={styles.list_item_container}>
                {redigerId === person.id ? (
                  <VirksomhetKontaktpersonSkjema orgnr={orgnr} person={person} onSubmit={reset} />
                ) : (
                  <div>
                    <Label size="small">Navn</Label>
                    <BodyShort size="small">{person.navn}</BodyShort>
                    <div className={styles.telefonepost_container}>
                      {person.telefon && (
                        <div>
                          <Label size="small">Telefon</Label>
                          <BodyShort size="small">{person.telefon}</BodyShort>
                        </div>
                      )}
                      <div>
                        <Label size="small">Epost</Label>
                        <BodyShort size="small">{person.epost}</BodyShort>
                      </div>
                    </div>
                    {person.beskrivelse && (
                      <>
                        <Label size="small">Beskrivelse</Label>
                        <BodyShort size="small">{person.beskrivelse}</BodyShort>
                      </>
                    )}
                  </div>
                )}
                <div className={styles.button_container}>
                  {redigerId !== person.id && (
                    <Button
                      size="small"
                      type="button"
                      onClick={() => {
                        setRedigerId(person.id);
                        setOpprett(false);
                      }}
                    >
                      Rediger
                    </Button>
                  )}
                </div>
              </div>
            ))}
          {opprett ? (
            <div className={styles.list_item_container}>
              <VirksomhetKontaktpersonSkjema orgnr={orgnr} onSubmit={reset} />
            </div>
          ) : (
            <Button
              className={styles.kontaktperson_button}
              size="small"
              type="button"
              onClick={() => {
                setOpprett(true);
                setRedigerId(undefined);
              }}
            >
              <PlusIcon aria-label="Opprett ny kontaktperson" /> eller opprett ny kontaktperson
            </Button>
          )}
        </div>
      </Modal.Body>
    </Modal>
  );
}
