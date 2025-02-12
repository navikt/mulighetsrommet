import { Button, Checkbox, CheckboxGroup, Heading, HGrid, Modal, Textarea } from "@navikt/ds-react";
import { useState } from "react";

interface Props<T> {
  open: boolean;
  header: string;
  buttonLabel: string;
  aarsaker: { label: string; value: T }[];
  onClose: () => void;
  onConfirm: (data: { aarsaker: T[]; forklaring?: string }) => void;
}

interface ValidationErrors {
  aarsak?: string;
  forklaring?: string;
}

const FORKLARING_MAX_LENGTH = 500;

export function AarsakerOgForklaringModal<T>(props: Props<T>) {
  const { open, onClose, onConfirm, header, buttonLabel, aarsaker } = props;
  const [valgteAarsaker, setValgteAarsaker] = useState<T[]>([]);
  const [forklaring, setForklaring] = useState<string | undefined>(undefined);
  const [errors, setErrors] = useState<ValidationErrors | null>(null);

  function validate() {
    const validationErrors: ValidationErrors = {};
    if (valgteAarsaker.length === 0) {
      validationErrors.aarsak = "Du må velge minst én årsak";
    }

    if (valgteAarsaker.some((aarsak) => String(aarsak) === "FEIL_ANNET") && !forklaring) {
      validationErrors.forklaring = "Du må skrive en forklaring når du velger 'Annet'";
    }

    if (forklaring && forklaring.length > FORKLARING_MAX_LENGTH) {
      validationErrors.forklaring = `Forklaringen kan ikke være lengre enn ${FORKLARING_MAX_LENGTH} tegn`;
    }

    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      return;
    } else {
      onConfirm({
        aarsaker: valgteAarsaker,
        forklaring: forklaring || undefined,
      });
    }
  }

  return (
    <Modal width={"medium"} aria-label={header} open={open} onClose={onClose}>
      <form>
        <Modal.Header>
          <Heading size="medium">{header}</Heading>
        </Modal.Header>
        <Modal.Body>
          <div className="bg-surface-hover p-6">
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
                {aarsaker.map(({ label, value }) => (
                  <Checkbox key={String(value)} value={value}>
                    {label}
                  </Checkbox>
                ))}
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
            {buttonLabel}
          </Button>
        </Modal.Footer>
      </form>
    </Modal>
  );
}
