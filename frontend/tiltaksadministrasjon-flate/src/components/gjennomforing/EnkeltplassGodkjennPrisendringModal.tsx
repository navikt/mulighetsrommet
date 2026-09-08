import { useGodkjennOkonomi } from "@/api/gjennomforing/useGodkjennOkonomi";
import { Button, InfoCard, Modal, VStack } from "@navikt/ds-react";
import { PrismodellDto } from "@tiltaksadministrasjon/api-client";
import { InformationSquareIcon } from "@navikt/aksel-icons";
import { Betalingsbetingelser } from "./Betalingsbetingelser";

interface Props {
  open: boolean;
  setOpen: (open: boolean) => void;
  gjennomforingId: string;
  totrinnskontrollId: string;
  prismodell: PrismodellDto;
}

export function EnkeltplassGodkjennPrisendringModal({
  open,
  setOpen,
  gjennomforingId,
  totrinnskontrollId,
  prismodell,
}: Props) {
  const godkjennMutation = useGodkjennOkonomi();

  function close() {
    setOpen(false);
  }

  function godkjenn() {
    godkjennMutation.mutate({ id: gjennomforingId, totrinnskontrollId }, { onSuccess: close });
  }

  return (
    <Modal open={open} onClose={close} header={{ heading: "Godkjenn prisendring" }} width="medium">
      <Modal.Body>
        <VStack gap="space-8">
          <InfoCard data-color="info">
            <InfoCard.Header icon={<InformationSquareIcon aria-hidden />}>
              <InfoCard.Title>
                Du er i ferd med å godkjenne de nye pris- og betalingsbetingelsene beskrevet under
              </InfoCard.Title>
            </InfoCard.Header>
            <InfoCard.Content>
              Når du godkjenner prisendringen, oppdateres prisen på enkeltplassen.
            </InfoCard.Content>
          </InfoCard>
          <Betalingsbetingelser prismodell={prismodell} />
        </VStack>
      </Modal.Body>
      <Modal.Footer>
        <Button size="small" onClick={godkjenn} loading={godkjennMutation.isPending}>
          Ja, godkjenn prisendring
        </Button>
        <Button size="small" variant="secondary" onClick={close}>
          Avbryt
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
