import { useSetApentForPamelding } from "@/api/gjennomforing/useSetApentForPamelding";
import { QueryKeys } from "@/api/QueryKeys";
import { GjennomforingDto } from "@mr/api-client-v2";
import { Button, Modal, Switch } from "@navikt/ds-react";
import { useQueryClient } from "@tanstack/react-query";
import { RefObject } from "react";

interface Props {
  modalRef: RefObject<HTMLDialogElement | null>;
  gjennomforing: GjennomforingDto;
}

export function SetApentForPameldingModal({ modalRef, gjennomforing }: Props) {
  const { mutate } = useSetApentForPamelding(gjennomforing.id);
  const queryClient = useQueryClient();

  return (
    <Modal ref={modalRef} header={{ heading: "Åpent for påmelding" }}>
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
                onSuccess: async () => {
                  await queryClient.invalidateQueries({
                    queryKey: QueryKeys.gjennomforing(gjennomforing.id),
                    refetchType: "all",
                  });
                },
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
