import { Button, HStack, InlineMessage, Modal, VStack } from "@navikt/ds-react";
import { TilsagnDeltakerRequest, TilsagnDeltakerDto } from "@tiltaksadministrasjon/api-client";
import { useState } from "react";
import { TilsagnDeltakereTable } from "./TilsagnDeltakereTable";

interface Props {
  deltakere: TilsagnDeltakerDto[];
  selectedDeltakere: TilsagnDeltakerRequest[];
  open: boolean;
  onClose: () => void;
  onBekreft: (deltakere: TilsagnDeltakerRequest[]) => void;
}

export function VelgDeltakereModal({
  deltakere,
  selectedDeltakere,
  open,
  onClose,
  onBekreft,
}: Props) {
  const [selected, setSelected] = useState<TilsagnDeltakerRequest[]>(selectedDeltakere);

  return (
    <Modal
      width={1100}
      closeOnBackdropClick
      onClose={onClose}
      open={open}
      header={{ heading: "Velg deltakere" }}
    >
      <Modal.Body className="max-h-[70vh] overflow-y-auto">
        <VStack gap="space-8">
          <InlineMessage status="info">
            Du kan velge deltakere som overlapper med tilsagnsperioden
          </InlineMessage>
          <TilsagnDeltakereTable
            deltakere={deltakere}
            selected={selected}
            setSelected={setSelected}
          />
        </VStack>
      </Modal.Body>
      <Modal.Footer>
        <HStack gap="space-4">
          <Button type="button" variant="tertiary" onClick={onClose}>
            Avbryt
          </Button>
          <Button type="button" onClick={() => onBekreft(selected)}>
            Bekreft
          </Button>
        </HStack>
      </Modal.Footer>
    </Modal>
  );
}
