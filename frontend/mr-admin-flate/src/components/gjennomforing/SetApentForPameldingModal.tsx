import { useSetApentForPamelding } from "@/api/gjennomforing/useSetApentForPamelding";
import { QueryKeys } from "@/api/QueryKeys";
import { BodyShort, Button, List, Modal, Switch } from "@navikt/ds-react";
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
          <BodyShort spacing>
            Her kan du styre om tiltaket skal være åpent for påmelding i Modia.
          </BodyShort>

          <BodyShort>Påmelding stenges automatisk av systemet når:</BodyShort>

          <List>
            <List.Item>
              Tiltak med <b>felles oppstart</b> starter.
            </List.Item>
            <List.Item>Tiltaket avsluttes eller blir avbrutt.</List.Item>
          </List>

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
