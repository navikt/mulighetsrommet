import { BodyShort, Button, Label, Modal } from "@navikt/ds-react";
import { VirksomhetKontaktperson } from "mulighetsrommet-api-client";
import { RefObject, useState } from "react";
import { useVirksomhetById } from "../../api/virksomhet/useVirksomhetById";
import { useVirksomhetKontaktpersoner } from "../../api/virksomhet/useVirksomhetKontaktpersoner";
import { Laster } from "../laster/Laster";
import { VirksomhetKontaktpersonSkjema } from "./VirksomhetKontaktpersonSkjema";
import styles from "./VirksomhetKontaktpersonerModal.module.scss";

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
        setOpprett(false);
        modalRef.current?.close();
      }}
      header={{
        heading: `Kontaktpersoner hos ${virksomhet.navn}`,
      }}
    >
      <Modal.Body>
        <div className={styles.modal_body}>
          <Button
            className={styles.kontaktperson_button}
            size="small"
            type="button"
            variant="primary"
            onClick={() => {
              setOpprett(true);
              setRedigerId(undefined);
            }}
          >
            Opprett ny kontaktperson
          </Button>
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
          ) : null}
        </div>
      </Modal.Body>
    </Modal>
  );
}
