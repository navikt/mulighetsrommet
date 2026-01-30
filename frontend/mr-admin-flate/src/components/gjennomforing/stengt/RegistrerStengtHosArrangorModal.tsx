import { RegistrerStengtHosArrangorForm } from "@/components/gjennomforing/stengt/RegistrerStengtHosArrangorForm";
import { StengtHosArrangorTable } from "@/components/gjennomforing/stengt/StengtHosArrangorTable";
import { Button, Modal, VStack } from "@navikt/ds-react";
import { RefObject, useState } from "react";
import { GjennomforingGruppetiltakStengtPeriode } from "@tiltaksadministrasjon/api-client";

interface Props {
  modalRef: RefObject<HTMLDialogElement | null>;
  gjennomforingId: string;
  stengt: GjennomforingGruppetiltakStengtPeriode[];
}

export function RegistrerStengtHosArrangorModal({ modalRef, gjennomforingId, stengt }: Props) {
  // This key is used to re-render the form when the modal is closed
  const [key, setKey] = useState(0);

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
        <VStack gap="4">
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
          <StengtHosArrangorTable gjennomforingId={gjennomforingId} stengt={stengt} />
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
