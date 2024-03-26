import { BodyShort, Button, Label, Modal } from "@navikt/ds-react";
import { RefObject, useState } from "react";
import { useArrangor } from "@/api/arrangor/useArrangor";
import { useArrangorKontaktpersoner } from "@/api/arrangor/useArrangorKontaktpersoner";
import { Laster } from "../laster/Laster";
import { ArrangorKontaktpersonSkjema } from "./ArrangorKontaktpersonSkjema";
import styles from "./ArrangorKontaktpersonerModal.module.scss";

interface Props {
  arrangorId: string;
  modalRef: RefObject<HTMLDialogElement>;
}

export function ArrangorKontaktpersonerModal(props: Props) {
  const { arrangorId, modalRef } = props;
  const { data: arrangor, isLoading: isLoadingArrangor } = useArrangor(arrangorId);
  const { data: kontaktpersoner, isLoading: isLoadingKontaktpersoner } =
    useArrangorKontaktpersoner(arrangorId);

  const [opprett, setOpprett] = useState<boolean>(false);
  const [redigerId, setRedigerId] = useState<string | undefined>(undefined);

  if (!arrangor || !kontaktpersoner || isLoadingKontaktpersoner || isLoadingArrangor) {
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
        heading: `Kontaktpersoner hos ${arrangor.navn}`,
      }}
    >
      <Modal.Body>
        <div className={styles.modal_body}>
          <Button
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
            .map((person) => (
              <div key={person.id} className={styles.list_item_container}>
                {redigerId === person.id ? (
                  <ArrangorKontaktpersonSkjema
                    arrangorId={arrangorId}
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
              <ArrangorKontaktpersonSkjema arrangorId={arrangorId} onSubmit={reset} />
            </div>
          ) : null}
        </div>
      </Modal.Body>
    </Modal>
  );
}
