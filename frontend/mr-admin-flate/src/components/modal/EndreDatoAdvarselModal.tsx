import { BodyShort, Button } from "@navikt/ds-react";
import { RefObject } from "react";
import { VarselModal } from "@/components/modal/VarselModal";

interface Props {
  modalRef: RefObject<HTMLDialogElement>;
  onCancel: () => void;
  antallDeltakere: number;
}

export function EndreDatoAdvarselModal({ modalRef, onCancel, antallDeltakere }: Props) {
  const onClose = () => {
    modalRef.current?.close();
  };

  return (
    <VarselModal
      modalRef={modalRef}
      handleClose={onClose}
      headingIconType="warning"
      headingText="Det finnes brukere påmeldt denne gjennomføringen"
      body={
        <BodyShort>
          {`Det finnes ${antallDeltakere} deltaker${antallDeltakere > 1 ? "e" : ""}
            på gjennomføringen. Ved å endre dato for gjennomføringen kan det medføre
            at datoer for deltakerne også oppdateres automatisk.`}
        </BodyShort>
      }
      secondaryButton
      secondaryButtonHandleAction={onCancel}
      primaryButton={
        <Button type="button" variant="danger" onClick={onClose}>
          Ja, jeg vil endre dato
        </Button>
      }
    />
  );
}
