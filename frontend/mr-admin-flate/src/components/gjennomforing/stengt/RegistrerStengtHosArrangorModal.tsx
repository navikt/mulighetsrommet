import { GjennomforingDto } from "@mr/api-client-v2";
import { Button, Modal } from "@navikt/ds-react";
import { RefObject } from "react";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { StengtHosArrangorTable } from "@/components/gjennomforing/stengt/StengtHosArrangorTable";
import { RegistrerStengtHosArrangorForm } from "@/components/gjennomforing/stengt/RegistrerStengtHosArrangorForm";

interface Props {
  modalRef: RefObject<HTMLDialogElement>;
  gjennomforing: GjennomforingDto;
}

export function RegistrerStengtHosArrangorModal({ modalRef, gjennomforing }: Props) {
  return (
    <Modal ref={modalRef} header={{ heading: "Registrer stengt hos arrangør" }} width={800}>
      <Modal.Body>
        <div className="prose">
          <p>Her kan du styre perioder der tiltaket er stengt hos arrangør.</p>

          <p>
            Arrangør vil ikke få betalt for deltakelser som er aktive i en periode der det er stengt
            hos arrangøren. Merk at deltakere fortsatt kan være påmeldt tiltaket med en aktiv
            status.
          </p>
        </div>

        <TwoColumnGrid separator>
          <RegistrerStengtHosArrangorForm gjennomforing={gjennomforing} />
          <StengtHosArrangorTable gjennomforing={gjennomforing} />
        </TwoColumnGrid>
      </Modal.Body>
      <Modal.Footer>
        <Button type="button" onClick={() => modalRef.current?.close()}>
          Lukk
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
