import { PlusIcon } from "@navikt/aksel-icons";
import { BodyShort, Button, Label, Modal } from "@navikt/ds-react";
import { RefObject, useState } from "react";
import { useVirksomhetKontaktpersoner } from "../../api/virksomhet/useVirksomhetKontaktpersoner";
import styles from "./VirksomhetKontaktpersonerModal.module.scss";
import { Laster } from "../laster/Laster";
import { VirksomhetKontaktperson } from "mulighetsrommet-api-client";
import { VirksomhetKontaktpersonSkjema } from "./VirksomhetKontaktpersonSkjema";
import { useVirksomhetById } from "../../api/virksomhet/useVirksomhet";

interface Props {
  virksomhetId: string;
  modalRef: RefObject<HTMLDialogElement>;
}

export function VirksomhetKontaktpersonerModal(props: Props) {
  const { virksomhetId, modalRef } = props;
  const { data: virksomhet, isLoading: isLoadingVirksomhet } = useVirksomhetById(virksomhetId);
  const { data: kontaktpersoner, isLoading: isLoadingKontaktpersoner } =
    useVirksomhetKontaktpersoner(virksomhetId);

  const [opprett, setOpprett] = useState<boolean>(false);
  const [redigerId, setRedigerId] = useState<string | undefined>(undefined);

  if (!virksomhet || !kontaktpersoner || isLoadingKontaktpersoner || isLoadingVirksomhet) {
    return <Laster />;
  }

  function reset() {
    setOpprett(false);
    setRedigerId(undefined);
  }

  return (
    <Modal
      ref={modalRef}
      className={styles.modal}
      onClose={() => {
        modalRef.current?.close();
      }}
      header={{
        heading: `Kontaktpersoner hos ${virksomhet.navn}`,
      }}
    >
      <Modal.Body>
        <div className={styles.modal_body}>
          {kontaktpersoner
            .sort((a, b) => a.navn.localeCompare(b.navn))
            .map((person: VirksomhetKontaktperson) => (
              <div key={person.id} className={styles.list_item_container}>
                {redigerId === person.id ? (
                  <VirksomhetKontaktpersonSkjema
                    virksomhetId={virksomhetId}
                    person={person}
                    onSubmit={reset}
                  />
                ) : (
                  <div>
                    <Label size="small">Navn</Label>
                    <BodyShort size="small">{person.navn}</BodyShort>
                    <div className={styles.telefonepost_container}>
                      {person.telefon && (
                        <div>
                          <Label size="small">Telefon</Label>
                          <BodyShort>
                            <a href={`tel:${person.telefon}`}>{person.telefon}</a>
                          </BodyShort>
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
              <VirksomhetKontaktpersonSkjema virksomhetId={virksomhetId} onSubmit={reset} />
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
