import { addOrRemove } from "@mr/frontend-common/utils/utils";
import { Button, HStack, InlineMessage, Modal } from "@navikt/ds-react";
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
      width={900}
      closeOnBackdropClick
      onClose={onClose}
      open={open}
      header={{ heading: "Velg deltakere" }}
    >
      <Modal.Body className="max-h-[70vh] overflow-y-auto">
        <InlineMessage status="info">
          Du kan velge deltakere som overlapper med tilsagnsperioden
        </InlineMessage>
        <TilsagnDeltakereTable
          deltakere={deltakere}
          selected={(d) => selected.some((s) => s.deltakerId === d.deltakerId)}
          onClick={(d) =>
            setSelected(
              addOrRemove(selected, { deltakerId: d.deltakerId, innholdAnnet: d.innholdAnnet }),
            )
          }
        />
      </Modal.Body>
      <Modal.Footer>
        <HStack gap="space-4" onClick={() => onBekreft(selected)} className="flex-row-reverse">
          <Button type="button">Bekreft</Button>
          <Button type="button" variant="tertiary" onClick={onClose}>
            Avbryt
          </Button>
        </HStack>
      </Modal.Footer>
    </Modal>
  );
}
