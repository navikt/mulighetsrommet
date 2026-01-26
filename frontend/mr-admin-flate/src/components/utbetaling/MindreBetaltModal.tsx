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
        heading: "Beløpet er mindre enn innsendt",
        icon: <InformationSquareFillIcon />,
      }}
    >
      <Modal.Body>
        <VStack gap="2">
          <VStack>
            <BodyShort>
              Beløpet du er i ferd med å sende til attestering er mindre en beløpet på utbetalingen.
              Er du sikker?
            </BodyShort>
            <BodyShort>Beløp til attestering: {formaterValutaBelop(belopUtbetaling)}</BodyShort>
            <BodyShort>Innsendt beløp: {formaterValutaBelop(belopInnsendt)}</BodyShort>
          </VStack>
          <Textarea
            label="Begrunnelse"
            onChange={begrunnelseOnChange}
            description="Oppgi begrunnelse for beløp som utbetales. Begrunnelsen vil kun være synlig for NAV"
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
