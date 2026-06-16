import { useGodkjennGjennomforingOkonomi } from "@/api/gjennomforing/useGodkjennGjennomforingOkonomi";
import { Button, InfoCard, Modal, VStack } from "@navikt/ds-react";
import { Betalingsbetingelser } from "../tilskudd-behandling/Betalingsbetingelser";
import { PrismodellDto } from "@tiltaksadministrasjon/api-client";
import { InformationSquareIcon } from "@navikt/aksel-icons";

interface Props {
  open: boolean;
  setOpen: (open: boolean) => void;
  gjennomforingId: string;
  prismodell: PrismodellDto;
}

export function GodkjennOkonomiModal({ open, setOpen, gjennomforingId, prismodell }: Props) {
  const godkjennMutation = useGodkjennGjennomforingOkonomi();

  function close() {
    setOpen(false);
  }

  function godkjenn() {
    godkjennMutation.mutate({ id: gjennomforingId }, { onSuccess: close });
  }

  return (
    <Modal open={open} onClose={close} header={{ heading: "Godkjenn enkeltplass" }} width="medium">
      <Modal.Body>
        <VStack gap="space-8">
          <InfoCard data-color="info">
            <InfoCard.Header icon={<InformationSquareIcon aria-hidden />}>
              <InfoCard.Title>
                Du er i ferd med å godkjenne pris og betalingsbetingelser som er beskrevet under
              </InfoCard.Title>
            </InfoCard.Header>
            <InfoCard.Content>
              Når du godkjenner økonomien, fattes det automatisk vedtak om tiltaksplass. Vedtaket
              inneholder informasjon om innhold, pris og betalingsbetingelser.
            </InfoCard.Content>
          </InfoCard>
          <Betalingsbetingelser prisbetingelser={prismodell.prisbetingelser} />
        </VStack>
      </Modal.Body>
      <Modal.Footer>
        <Button size="small" onClick={godkjenn} loading={godkjennMutation.isPending}>
          Ja, godkjenn enkeltplass
        </Button>
        <Button size="small" variant="secondary" onClick={close}>
          Avbryt
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
