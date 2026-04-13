import { Button, Heading, Spacer } from "@navikt/ds-react";
import { ValideringsfeilOppsummering } from "./ValideringsfeilOppsummering";
import { FormButtonsContainer } from "./FormButtonsContainer";
import { useNavigate } from "react-router";

interface Props {
  heading?: string;
  cancelLabel?: string;
  submitLabel?: string;
  onCancel?: () => void;
  isPending?: boolean;
}

export function FormButtons({
  heading,
  cancelLabel = "Avbryt",
  submitLabel = "Lagre",
  onCancel,
  isPending = false,
}: Props) {
  const navigate = useNavigate();

  function handleCancel() {
    if (onCancel) {
      onCancel();
    } else {
      navigate(-1);
    }
  }

  return (
    <FormButtonsContainer>
      {heading && (
        <Heading level="2" size="medium">
          {heading}
        </Heading>
      )}
      <Spacer />
      <ValideringsfeilOppsummering />
      <Button size="small" onClick={handleCancel} variant="tertiary" type="button" disabled={isPending}>
        {cancelLabel}
      </Button>
      <Button size="small" type="submit" disabled={isPending} loading={isPending}>
        {submitLabel}
      </Button>
    </FormButtonsContainer>
  );
}
