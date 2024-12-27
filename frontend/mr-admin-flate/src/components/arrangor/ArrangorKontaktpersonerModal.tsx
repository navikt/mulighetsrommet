import { useArrangor } from "@/api/arrangor/useArrangor";
import { useArrangorKontaktpersoner } from "@/api/arrangor/useArrangorKontaktpersoner";
import { BodyShort, Button, HGrid, HStack, Label, Modal, Tag, VStack } from "@navikt/ds-react";
import { ArrangorKontaktperson } from "@mr/api-client";
import { RefObject, useState } from "react";
import { Laster } from "../laster/Laster";
import { ArrangorKontaktpersonSkjema } from "./ArrangorKontaktpersonSkjema";
import { navnForAnsvar } from "./ArrangorKontaktpersonUtils";
import styles from "./ArrangorKontaktpersonerModal.module.scss";

interface Props {
  arrangorId: string;
  modalRef: RefObject<HTMLDialogElement>;
  onOpprettSuccess: (kontaktperson: ArrangorKontaktperson) => void;
}

export function ArrangorKontaktpersonerModal(props: Props) {
  const { arrangorId, modalRef, onOpprettSuccess } = props;
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
          {kontaktpersoner
            .sort((a, b) => a.navn.localeCompare(b.navn))
            .map((person) => (
              <div key={person.id} className={styles.list_item_container}>
                {redigerId === person.id ? (
                  <ArrangorKontaktpersonSkjema
                    arrangorId={arrangorId}
                    person={person}
                    onSubmit={reset}
                    onOpprettSuccess={() => {}}
                  />
                ) : (
                  <VStack gap="2">
                    <div>
                      <Label size="small">Navn</Label>
                      <BodyShort size="small">{person.navn}</BodyShort>
                    </div>
                    <div>
                      <HGrid columns={2}>
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
                      </HGrid>
                    </div>
                    <div>
                      <HGrid columns={2}>
                        {person.ansvarligFor && (
                          <div>
                            <Label size="small">Ansvarlig for</Label>
                            <HStack gap="5">
                              <ul className={styles.list_unstyled}>
                                {person.ansvarligFor.map((ansvar) => (
                                  <li key={ansvar}>
                                    <Tag variant="info" size="xsmall">
                                      {navnForAnsvar(ansvar)}
                                    </Tag>
                                  </li>
                                ))}
                              </ul>
                            </HStack>
                          </div>
                        )}
                        {person.beskrivelse && (
                          <div>
                            <Label size="small">Beskrivelse</Label>
                            <BodyShort size="small">{person.beskrivelse}</BodyShort>
                          </div>
                        )}
                      </HGrid>
                    </div>
                  </VStack>
                )}
                <VStack justify="start" align="start" className={styles.button_container}>
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
                </VStack>
              </div>
            ))}
          {opprett ? (
            <div className={styles.list_item_container}>
              <ArrangorKontaktpersonSkjema
                arrangorId={arrangorId}
                onSubmit={reset}
                onOpprettSuccess={onOpprettSuccess}
              />
            </div>
          ) : null}
        </div>
      </Modal.Body>
      <Modal.Footer>
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
      </Modal.Footer>
    </Modal>
  );
}
