import { useSettOkonomiPaVent } from "@/api/gjennomforing/useSettOkonomiPaVent";
import { InformationSquareIcon } from "@navikt/aksel-icons";
import { Button, InfoCard, Modal, Textarea, VStack } from "@navikt/ds-react";
import { useState } from "react";

interface Props {
  open: boolean;
  setOpen: (open: boolean) => void;
  gjennomforingId: string;
  totrinnskontrollId: string;
}

export function EnkeltplassSettOkonomiPaVentModal({
  open,
  setOpen,
  gjennomforingId,
  totrinnskontrollId,
}: Props) {
  const settPaVentMutation = useSettOkonomiPaVent();
  const [begrunnelse, setBegrunnelse] = useState("");

  function close() {
    setOpen(false);
    setBegrunnelse("");
  }

  function settPaVent() {
    settPaVentMutation.mutate(
      { id: gjennomforingId, begrunnelse: begrunnelse || null, totrinnskontrollId },
      { onSuccess: close },
    );
  }

  return (
    <Modal
      open={open}
      onClose={close}
      header={{ heading: "Sett enkeltplass på vent" }}
      width="medium"
    >
      <Modal.Body>
        <VStack gap="space-8">
          <InfoCard data-color="info">
            <InfoCard.Header icon={<InformationSquareIcon aria-hidden />}>
              <InfoCard.Title>
                Du er i ferd med å sette godkjenning av enkeltplassen på vent
              </InfoCard.Title>
            </InfoCard.Header>
            <InfoCard.Content>
              For at veileder skal få beskjed, må du sende en oppgave i Gosys med beskrivelse av hva
              som er mangelfullt i påmeldingen.
            </InfoCard.Content>
          </InfoCard>
          <Textarea
            label="Intern kommentar (valgfritt)"
            value={begrunnelse}
            onChange={(e) => setBegrunnelse(e.target.value)}
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
