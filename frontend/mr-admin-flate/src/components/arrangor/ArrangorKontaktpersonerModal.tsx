import { useArrangor } from "@/api/arrangor/useArrangor";
import { useArrangorKontaktpersoner } from "@/api/arrangor/useArrangorKontaktpersoner";
import { BodyShort, Button, HGrid, HStack, Label, Modal, Tag, VStack } from "@navikt/ds-react";
import { ArrangorKontaktperson } from "@tiltaksadministrasjon/api-client";
import { RefObject, useState } from "react";
import { Laster } from "../laster/Laster";
import { ArrangorKontaktpersonForm } from "./ArrangorKontaktpersonForm";
import { navnForAnsvar } from "./ArrangorKontaktpersonUtils";

interface Props {
  arrangorId: string;
  modalRef: RefObject<HTMLDialogElement | null>;
  onOpprettSuccess: (kontaktperson: ArrangorKontaktperson) => void;
}

export function ArrangorKontaktpersonerModal(props: Props) {
  const { arrangorId, modalRef, onOpprettSuccess } = props;
  const { data: arrangor } = useArrangor(arrangorId);
  const { data: kontaktpersoner } = useArrangorKontaktpersoner(arrangorId);

  const [opprett, setOpprett] = useState<boolean>(false);
  const [redigerId, setRedigerId] = useState<string | undefined>(undefined);

  if (!arrangor || !kontaktpersoner) {
    return <Laster />;
  }

  function reset() {
    setOpprett(false);
    setRedigerId(undefined);
  }

  return (
    <Modal
      ref={modalRef}
      width={500}
      onClose={() => {
        setOpprett(false);
        modalRef.current?.close();
      }}
      header={{
        heading: `Kontaktpersoner hos ${arrangor.navn}`,
      }}
    >
      <Modal.Body>
        <div className="p-2 flex flex-col max-h-[600px] overflow-auto gap-4">
          {kontaktpersoner
            .sort((a, b) => a.navn.localeCompare(b.navn))
            .map((person) => (
              <div
                key={person.id}
                className="p-4 border border-border-divider bg-gray-100 rounded-l"
              >
                {redigerId === person.id ? (
                  <ArrangorKontaktpersonForm
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
                        <div>
                          <Label size="small">Ansvarlig for</Label>
                          <HStack gap="5">
                            <ul className="list-none flex gap-2 p-0 m-0">
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
                <VStack
                  justify="start"
                  align="start"
                  className="mt-[8px] flex flex-row justify-between"
                >
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
            <div className="p-4 border border-border-divider bg-gray-100 rounded-l">
              <ArrangorKontaktpersonForm
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
