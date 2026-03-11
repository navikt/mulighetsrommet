import { formaterValutaBelop } from "@mr/frontend-common/utils/utils";
import { InformationSquareFillIcon } from "@navikt/aksel-icons";
import { BodyShort, Button, Modal, Textarea, VStack } from "@navikt/ds-react";
import { ValutaBelop } from "@tiltaksadministrasjon/api-client";
import { ChangeEventHandler } from "react";

export default function MindreBelopModal({
  open,
  handleClose,
  onConfirm,
  belopInnsendt,
  belopUtbetaling,
  begrunnelseOnChange,
}: {
  open: boolean;
  handleClose: () => void;
  onConfirm: () => void;
  belopInnsendt: ValutaBelop;
  belopUtbetaling: ValutaBelop;
  begrunnelseOnChange: ChangeEventHandler<HTMLTextAreaElement>;
}) {
  return (
    <Modal
      open={open}
      className="text-left"
      onClose={handleClose}
      header={{
        heading: "Beløp til utbetaling er mindre enn innsendt beløp",
        icon: <InformationSquareFillIcon color="var(--ax-text-info-subtle)" />,
      }}
    >
      <Modal.Body>
        <VStack gap="space-8">
          <VStack>
            <BodyShort spacing>
              Beløpet du er i ferd med å sende til attestering er mindre enn beløpet på
              utbetalingen. Er du sikker på at du vil fortsette?
            </BodyShort>
            <BodyShort>Beløp til attestering: {formaterValutaBelop(belopUtbetaling)}</BodyShort>
            <BodyShort>Innsendt beløp: {formaterValutaBelop(belopInnsendt)}</BodyShort>
          </VStack>
          <Textarea
            label="Begrunnelse"
            onChange={begrunnelseOnChange}
            description="Oppgi begrunnelse for beløpet som utbetales. Begrunnelsen vil kun være synlig for Nav."
          />
        </VStack>
      </Modal.Body>
      <Modal.Footer>
        <Button variant="primary" onClick={onConfirm}>
          Ja, send til attestering
        </Button>
        <Button variant="secondary" onClick={handleClose}>
          Avbryt
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
