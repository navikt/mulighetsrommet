import { RegistrerStengtHosArrangorForm } from "@/components/gjennomforing/stengt/RegistrerStengtHosArrangorForm";
import { StengtHosArrangorTable } from "@/components/gjennomforing/stengt/StengtHosArrangorTable";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { GjennomforingDto } from "@mr/api-client-v2";
import { Button, Modal } from "@navikt/ds-react";
import { RefObject, useState } from "react";

interface Props {
  modalRef: RefObject<HTMLDialogElement>;
  gjennomforing: GjennomforingDto;
}

export function RegistrerStengtHosArrangorModal({ modalRef, gjennomforing }: Props) {
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
      width={1200}
    >
      <Modal.Body>
        <div className="prose">
          <p>
            Her kan du legge inn perioder der tiltakstilbudet er stengt hos arrangør, for eksempel
            ved stengt i en sommerperiode.
          </p>

          <p>
            Arrangør vil ikke få betalt for deltakelser som er aktive i en periode der det er stengt
            hos arrangøren. Merk at deltakere fortsatt kan være påmeldt tiltaket med en aktiv
            status.
          </p>
        </div>

        <TwoColumnGrid separator>
          <RegistrerStengtHosArrangorForm key={key} gjennomforing={gjennomforing} />
          <StengtHosArrangorTable gjennomforing={gjennomforing} />
        </TwoColumnGrid>
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
