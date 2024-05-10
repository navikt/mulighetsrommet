import { BodyShort, Button } from "@navikt/ds-react";
import { RefObject } from "react";
import { VarselModal } from "@/components/modal/VarselModal";

interface Props {
  modalRef: RefObject<HTMLDialogElement>;
  onRediger: () => void;
  ressursNavn: string;
}

export function RedigeringsAdvarselModal({ modalRef, onRediger, ressursNavn }: Props) {
  const onClose = () => {
    modalRef.current?.close();
  };

  return (
    <VarselModal
      modalRef={modalRef}
      handleClose={onClose}
      headingIconType="warning"
      headingText={`Du er ikke eier av denne ${ressursNavn}`}
      body={<BodyShort>Vil du fortsette til redigeringen?</BodyShort>}
      secondaryButton
      primaryButton={
        <Button variant="primary" onClick={onRediger}>
          Ja, jeg vil redigere
        </Button>
      }
    />
  );
}
