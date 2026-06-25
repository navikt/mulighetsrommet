import { useSettPaVentGjennomforingOkonomi } from "@/api/gjennomforing/useSettPaVentGjennomforingOkonomi";
import { InformationSquareIcon } from "@navikt/aksel-icons";
import { Button, InfoCard, Modal, Textarea, VStack } from "@navikt/ds-react";
import { useState } from "react";

interface Props {
  open: boolean;
  setOpen: (open: boolean) => void;
  gjennomforingId: string;
}

export function SettPaVentPrisendringModal({ open, setOpen, gjennomforingId }: Props) {
  const settPaVentMutation = useSettPaVentGjennomforingOkonomi();
  const [forklaring, setForklaring] = useState("");

  function close() {
    setOpen(false);
    setForklaring("");
  }

  function settPaVent() {
    settPaVentMutation.mutate(
      { id: gjennomforingId, forklaring: forklaring || null },
      { onSuccess: close },
    );
  }

  return (
    <Modal
      open={open}
      onClose={close}
      header={{ heading: "Sett prisendring på vent" }}
      width="medium"
    >
      <Modal.Body>
        <VStack gap="space-8">
          <InfoCard data-color="info">
            <InfoCard.Header icon={<InformationSquareIcon aria-hidden />}>
              <InfoCard.Title>
                Du er i ferd med å sette godkjenning av prisendringen på vent
              </InfoCard.Title>
            </InfoCard.Header>
            <InfoCard.Content>
              For at veileder skal få beskjed, må du sende en oppgave i Gosys med beskrivelse av hva
              som er mangelfullt.
            </InfoCard.Content>
          </InfoCard>
          <Textarea
            label="Intern kommentar (valgfritt)"
            value={forklaring}
            onChange={(e) => setForklaring(e.target.value)}
            maxLength={500}
          />
        </VStack>
      </Modal.Body>
      <Modal.Footer>
        <Button
          size="small"
          variant="primary"
          onClick={settPaVent}
          loading={settPaVentMutation.isPending}
        >
          Sett på vent
        </Button>
        <Button size="small" variant="secondary" onClick={close}>
          Avbryt
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
