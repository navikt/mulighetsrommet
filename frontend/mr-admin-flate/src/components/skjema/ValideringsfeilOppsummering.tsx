import { extractValidationErrors } from "@/utils/Utils";
import { ExclamationmarkTriangleFillIcon } from "@navikt/aksel-icons";
import { Button, ErrorSummary, Popover } from "@navikt/ds-react";
import { useRef, useState } from "react";
import { useFormContext } from "react-hook-form";

export function ValideringsfeilOppsummering() {
  const buttonRef = useRef<HTMLButtonElement>(null);
  const [isOpen, setIsOpen] = useState(false);
  const {
    formState: { errors },
  } = useFormContext();

  const messages = extractValidationErrors(errors);

  if (messages.length === 0) return null;

  return (
    <>
      <Button
        data-color="neutral"
        icon={
          <ExclamationmarkTriangleFillIcon color="var(--ax-text-danger-subtle)" title="Rediger" />
        }
        variant="tertiary"
        type="button"
        size="small"
        onClick={() => setIsOpen(true)}
        ref={buttonRef}
        title="Se valideringsfeil"
      />
      <Popover open={isOpen} onClose={() => setIsOpen(false)} anchorEl={buttonRef.current}>
        <Popover.Content>
          <ErrorSummary heading="Du må rette følgende feil i skjemaet før du kan fortsette:">
            {messages.map((message, index) => (
              <ErrorSummary.Item key={index} className="no-underline text-ax-text-accent">
                {message.message}
              </ErrorSummary.Item>
            ))}
          </ErrorSummary>
        </Popover.Content>
      </Popover>
    </>
  );
}
