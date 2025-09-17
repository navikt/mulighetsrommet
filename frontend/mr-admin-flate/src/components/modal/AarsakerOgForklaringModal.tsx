import { FieldError } from "@mr/api-client-v2";
import {
  Alert,
  Button,
  Checkbox,
  CheckboxGroup,
  Heading,
  HGrid,
  HStack,
  Modal,
  Textarea,
  VStack,
} from "@navikt/ds-react";
import { useState } from "react";

interface Props<T> {
  open: boolean;
  header: string;
  ingress?: React.ReactNode;
  buttonLabel: string;
  aarsaker: { label: string; value: T }[];
  errors?: FieldError[];
  onClose: () => void;
  onConfirm: (data: { aarsaker: T[]; forklaring: string | null }) => void;
}

const FORKLARING_MAX_LENGTH = 500;

export function AarsakerOgForklaringModal<T>(props: Props<T>) {
  const { errors = [], open, onClose, onConfirm, header, ingress, buttonLabel, aarsaker } = props;
  const [valgteAarsaker, setValgteAarsaker] = useState<T[]>([]);
  const [forklaring, setForklaring] = useState<string | undefined>(undefined);

  return (
    <Modal width={1000} aria-label={header} open={open} onClose={onClose} portal={true}>
      <form>
        <Modal.Header>
          <VStack gap="4">
            <Heading size="medium">{header}</Heading>
            {ingress}
          </VStack>
        </Modal.Header>
        <Modal.Body>
          <HGrid columns={2} gap="6" align="start">
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
              onChange={(val) => setForklaring(val.currentTarget.value)}
              label="Forklaring"
              resize
              maxLength={FORKLARING_MAX_LENGTH}
            ></Textarea>
          </HGrid>
        </Modal.Body>
        <Modal.Footer>
          <VStack gap="2">
            <HStack justify="end">
              <Button
                type="submit"
                variant="primary"
                onClick={(e) => {
                  e.preventDefault();
                  onConfirm({ aarsaker: valgteAarsaker, forklaring: forklaring ?? null });
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
