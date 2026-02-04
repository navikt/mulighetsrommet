import { useSetApentForPamelding } from "@/api/gjennomforing/useSetApentForPamelding";
import { QueryKeys } from "@/api/QueryKeys";
import { Button, Modal, Switch } from "@navikt/ds-react";
import { useQueryClient } from "@tanstack/react-query";
import { RefObject } from "react";
import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import { isGruppetiltak } from "@/api/gjennomforing/utils";

interface Props {
  modalRef: RefObject<HTMLDialogElement | null>;
  gjennomforingId: string;
}

export function SetApentForPameldingModal({ modalRef, gjennomforingId }: Props) {
  const { mutate } = useSetApentForPamelding(gjennomforingId);
  const { gjennomforing } = useGjennomforing(gjennomforingId);
  const queryClient = useQueryClient();

  if (!isGruppetiltak(gjennomforing)) {
    return null;
  }

  const invalidateGjennomforing = async () => {
    await queryClient.invalidateQueries({
      queryKey: QueryKeys.gjennomforing(gjennomforingId),
      refetchType: "all",
    });
  };

  return (
    <Modal ref={modalRef} header={{ heading: "Åpent for påmelding" }} width={1000}>
      <Modal.Body className="prose">
        <div>
          <p>Her kan du styre om tiltaket skal være åpent for påmelding i Modia.</p>

          <p>Påmelding stenges automatisk av systemet når:</p>
          <ul>
            <li>
              Tiltak med <b>felles oppstart</b> starter.
            </li>
            <li>Tiltaket avsluttes eller blir avbrutt.</li>
          </ul>

          <Switch
            checked={gjennomforing.apentForPamelding}
            onChange={(e) =>
              mutate(e.target.checked, {
                onSuccess: invalidateGjennomforing,
              })
            }
          >
            Åpent for påmelding
          </Switch>
        </div>
      </Modal.Body>
      <Modal.Footer>
        <Button type="button" onClick={() => modalRef.current?.close()}>
          Lukk
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
