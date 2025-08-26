import { BodyLong, Box } from "@navikt/ds-react";

interface Props {
  text: string;
  className?: string;
  ariaLabel?: string;
}

export function Fritekstfelt({ text, ariaLabel, className = "" }: Props) {
  return (
    <Box
      borderColor="border-subtle"
      padding="2"
      borderWidth="1"
      borderRadius="medium"
      className={`h-20 overflow-y-auto resize-y ${className}`}
      aria-label={ariaLabel}
      tabIndex={0}
    >
      <BodyLong className="whitespace-pre-line">{text}</BodyLong>
    </Box>
  );
}
