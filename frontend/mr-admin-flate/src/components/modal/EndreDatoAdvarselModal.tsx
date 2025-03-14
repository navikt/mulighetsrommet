import { BodyShort, Button } from "@navikt/ds-react";
import { RefObject } from "react";
import { VarselModal } from "@mr/frontend-common/components/varsel/VarselModal";

interface Props {
  modalRef: RefObject<HTMLDialogElement | null>;
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
      headingIconType="info"
      headingText="Det finnes deltakere påmeldt denne gjennomføringen"
      body={
        <BodyShort>
          {`Det finnes ${antallDeltakere} deltaker${antallDeltakere > 1 ? "e" : ""}
           på gjennomføringen. Dersom det er deltakere med en sluttdato etter ny sluttdato
            på gjennomføringen, så vil deltakerne få sluttdato lik gjennomføringen sin sluttdato.`}
        </BodyShort>
      }
      secondaryButton
      secondaryButtonHandleAction={onCancel}
      primaryButton={
        <Button type="button" variant="primary" onClick={onClose}>
          Ja, jeg vil endre dato
        </Button>
      }
    />
  );
}
