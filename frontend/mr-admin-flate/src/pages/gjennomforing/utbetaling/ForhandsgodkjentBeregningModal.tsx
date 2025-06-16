import { Heading, Modal, VStack } from "@navikt/ds-react";
import { DeltakerForKostnadsfordeling } from "@mr/api-client-v2";
import { FilterableForhandsgodkjentDeltakerTable } from "@/components/utbetaling/FilterableForhandsgodkjentDeltakerTable";

interface Props {
  modalOpen: boolean;
  onClose: () => void;
  deltakere: DeltakerForKostnadsfordeling[];
  sats: number;
  heading: string;
}

export function ForhandsgodkjentBeregningModal({
  heading,
  sats,
  deltakere,
  modalOpen,
  onClose,
}: Props) {
  return (
    <Modal open={modalOpen} onClose={onClose} aria-label="modal" width="80rem">
      <Modal.Header closeButton>
        <Heading size="medium">Beregning</Heading>
      </Modal.Header>
      <Modal.Body>
        <VStack>
          <Heading size="small">{heading}</Heading>
          <FilterableForhandsgodkjentDeltakerTable deltakere={deltakere} sats={sats} />
        </VStack>
      </Modal.Body>
    </Modal>
  );
}
