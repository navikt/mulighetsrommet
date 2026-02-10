import { RegistrerStengtHosArrangorForm } from "@/components/gjennomforing/stengt/RegistrerStengtHosArrangorForm";
import { StengtHosArrangorTable } from "@/components/gjennomforing/stengt/StengtHosArrangorTable";
import { Button, Modal, VStack } from "@navikt/ds-react";
import { RefObject, useState } from "react";
import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import { isGruppetiltak } from "@/api/gjennomforing/utils";

interface Props {
  modalRef: RefObject<HTMLDialogElement | null>;
  gjennomforingId: string;
}

export function RegistrerStengtHosArrangorModal({ modalRef, gjennomforingId }: Props) {
  const { gjennomforing } = useGjennomforing(gjennomforingId);

  // This key is used to re-render the form when the modal is closed
  const [key, setKey] = useState(0);

  if (!isGruppetiltak(gjennomforing)) {
    return null;
  }

  function handleCloseModal() {
    modalRef.current?.close();
    setKey((prev) => prev + 1);
  }

  return (
    <Modal
      onClose={handleCloseModal}
      ref={modalRef}
      header={{ heading: "Registrer stengt periode hos arrangør" }}
      width={1000}
    >
      <Modal.Body>
        <VStack gap="space-16">
          <div className="prose">
            <p>
              Her kan du legge inn perioder der tiltakstilbudet er stengt hos arrangør, for eksempel
              ved stengt i en sommerperiode.
            </p>
            <br></br>
            <p>
              Hvis en prismodell med automatisk beregning basert på deltakelse er brukt, vil
              arrangør ikke få betalt for deltakelser som er aktive i disse periodene. Merk at
              deltakere fortsatt kan være påmeldt tiltaket med en aktiv status.
            </p>
          </div>
          <RegistrerStengtHosArrangorForm key={key} gjennomforingId={gjennomforingId} />
          <StengtHosArrangorTable gjennomforing={gjennomforing} />
        </VStack>
      </Modal.Body>
      <Modal.Footer>
        <Button
          size="small"
          type="button"
          onClick={() => {
            handleCloseModal();
          }}
        >
          Lukk
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
