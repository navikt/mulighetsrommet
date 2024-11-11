import {
  AvvistTilsagnRequest,
  TilsagnAvvisningAarsak,
  TilsagnBesluttelseStatus,
} from "@mr/api-client";
import { Button, Checkbox, CheckboxGroup, Heading, HGrid, Modal, Textarea } from "@navikt/ds-react";
import { useState } from "react";
import styles from "./AvvisTilsagnModal.module.scss";

interface Props {
  open: boolean;
  onClose: () => void;
  onConfirm: (validatedData: AvvistTilsagnRequest) => void;
}

interface ValidationErrors {
  aarsak?: string;
  forklaring?: string;
}

const FORKLARING_MAX_LENGTH = 500;

export function AvvisTilsagnModal({ open, onClose, onConfirm }: Props) {
  const [valgteAarsaker, setValgteAarsaker] = useState<TilsagnAvvisningAarsak[]>([]);
  const [forklaring, setForklaring] = useState<string>("");
  const [errors, setErrors] = useState<ValidationErrors | null>(null);

  function validate() {
    const validationErrors: ValidationErrors = {};
    if (valgteAarsaker.length === 0) {
      validationErrors.aarsak = "Du må velge minst én årsak for retur av tilsagnet";
    }

    if (valgteAarsaker.includes(TilsagnAvvisningAarsak.FEIL_ANNET) && !forklaring) {
      validationErrors.forklaring = "Du må skrive en forklaring når du velger 'Annet'";
    }

    if (forklaring.length > FORKLARING_MAX_LENGTH) {
      validationErrors.forklaring = `Forklaringen kan ikke være lengre enn ${FORKLARING_MAX_LENGTH} tegn`;
    }

    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      return;
    } else {
      onConfirm({
        besluttelse: TilsagnBesluttelseStatus.AVVIST,
        aarsaker: valgteAarsaker,
        forklaring,
      });
    }
  }

  return (
    <Modal width={"medium"} aria-label="Avvis tilsagn med forklaring" open={open} onClose={onClose}>
      <form>
        <Modal.Header>
          <Heading size="medium">Send i retur med forklaring</Heading>
        </Modal.Header>
        <Modal.Body>
          <div className={styles.body}>
            <HGrid columns={2} align="start">
              <CheckboxGroup
                onChange={(val) => {
                  setErrors(null);
                  setValgteAarsaker(val);
                }}
                value={valgteAarsaker}
                name="aarsak"
                legend="Årsak"
                error={errors?.aarsak}
              >
                <Checkbox value="FEIL_ANTALL_PLASSER">Feil i antall plasser</Checkbox>
                <Checkbox value="FEIL_KOSTNADSSTED">Feil kostnadssted</Checkbox>
                <Checkbox value="FEIL_PERIODE">Feil periode</Checkbox>
                <Checkbox value="FEIL_BELOP">Feil beløp</Checkbox>
                <Checkbox value="FEIL_ANNET">Annet</Checkbox>
              </CheckboxGroup>
              <Textarea
                error={errors?.forklaring}
                onChange={(val) => {
                  setErrors(null);
                  setForklaring(val.currentTarget.value);
                }}
                label="Forklaring"
                resize
                maxLength={FORKLARING_MAX_LENGTH}
              ></Textarea>
            </HGrid>
          </div>
        </Modal.Body>
        <Modal.Footer>
          <Button
            type="submit"
            variant="primary"
            onClick={(e) => {
              e.preventDefault();
              validate();
            }}
          >
            Send i retur med forklaring
          </Button>
        </Modal.Footer>
      </form>
    </Modal>
  );
}
