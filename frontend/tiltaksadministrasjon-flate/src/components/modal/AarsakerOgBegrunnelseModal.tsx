import { InformationSquareIcon } from "@navikt/aksel-icons";
import {
  Alert,
  Button,
  Checkbox,
  CheckboxGroup,
  HGrid,
  HStack,
  Modal,
  Textarea,
  VStack,
} from "@navikt/ds-react";
import { FieldError } from "@tiltaksadministrasjon/api-client";
import { useState } from "react";

interface Props<T> {
  width?: number;
  open: boolean;
  onClose: () => void;
  header: string;
  ingress?: React.ReactNode;
  buttonLabel: string;
  textareaLabel?: string;
  aarsaker: { label: string; value: T }[];
  errors?: FieldError[];
  onConfirm: (data: { aarsaker: T[]; begrunnelse: string | null }) => void;
}

const BEGRUNNELSE_MAX_LENGTH = 500;

export function AarsakerOgBegrunnelseModal<T>(props: Props<T>) {
  const {
    errors = [],
    width = 1_000,
    open,
    onClose,
    onConfirm,
    header,
    ingress,
    buttonLabel,
    textareaLabel = "Begrunnelse",
    aarsaker,
  } = props;
  const [valgteAarsaker, setValgteAarsaker] = useState<T[]>([]);
  const [begrunnelse, setBegrunnelse] = useState<string | undefined>(undefined);

  return (
    <Modal
      size="small"
      width={width}
      aria-label={header}
      open={open}
      onClose={onClose}
      portal={true}
      header={{
        icon: <InformationSquareIcon aria-hidden />,
        heading: header,
      }}
    >
      <form>
        <Modal.Body>
          <VStack gap="space-16">
            {ingress}
            <HGrid columns={2} gap="space-24" align="start">
              <CheckboxGroup
                onChange={setValgteAarsaker}
                value={valgteAarsaker}
                name="aarsak"
                legend="Årsak"
              >
                {aarsaker.map(({ label, value }) => (
                  <Checkbox key={String(value)} value={value}>
                    {label}
                  </Checkbox>
                ))}
              </CheckboxGroup>
              <Textarea
                onChange={(val) => setBegrunnelse(val.currentTarget.value)}
                label={textareaLabel}
                resize
                maxLength={BEGRUNNELSE_MAX_LENGTH}
              ></Textarea>
            </HGrid>
          </VStack>
        </Modal.Body>
        <Modal.Footer>
          <VStack gap="space-8">
            <HStack justify="end" gap="space-16">
              <Button
                variant="secondary"
                onClick={(e) => {
                  e.preventDefault();
                  onClose();
                }}
              >
                Avbryt
              </Button>
              <Button
                type="submit"
                variant="primary"
                onClick={(e) => {
                  e.preventDefault();
                  onConfirm({ aarsaker: valgteAarsaker, begrunnelse: begrunnelse || null });
                }}
              >
                {buttonLabel}
              </Button>
            </HStack>
            {errors.map((error) => (
              <Alert className="self-end" variant="error" size="small">
                {error.detail}
              </Alert>
            ))}
          </VStack>
        </Modal.Footer>
      </form>
    </Modal>
  );
}
