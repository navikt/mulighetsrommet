import { extractValidationErrors, ValidationMessage } from "@/utils/Utils";
import { ExclamationmarkTriangleFillIcon } from "@navikt/aksel-icons";
import { Button, ErrorSummary, Popover } from "@navikt/ds-react";
import { FieldError } from "@tiltaksadministrasjon/api-client";
import { useRef, useState } from "react";
import { useFormContext } from "react-hook-form";

export function ValideringsfeilOppsummering() {
  const {
    formState: { errors },
  } = useFormContext();

  const messages: ValidationMessage[] = extractValidationErrors(errors);

  return (
    <ValdationMessageSummary
      heading="Du må rette følgende feil i skjemaet før du kan fortsette:"
      messages={messages}
    />
  );
}

export function ErrorFieldSummary({ errors }: { errors: FieldError[] }) {
  const messages: ValidationMessage[] = errors.map((e) => ({ message: e.detail, ref: e.pointer }));

  return <ValdationMessageSummary heading="Det oppstod følgende feil:" messages={messages} />;
}

interface ValdationMessageSummaryProps {
  heading: string;
  messages: ValidationMessage[];
}

function ValdationMessageSummary({ messages, heading }: ValdationMessageSummaryProps) {
  const buttonRef = useRef<HTMLButtonElement>(null);
  const [isOpen, setIsOpen] = useState(false);

  if (messages.length === 0) return null;

  return (
    <>
      <Button
        data-color="neutral"
        icon={
          <ExclamationmarkTriangleFillIcon
            color="var(--ax-text-danger-subtle)"
            title="Feil indikator"
          />
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
          <ErrorSummary heading={heading}>
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
