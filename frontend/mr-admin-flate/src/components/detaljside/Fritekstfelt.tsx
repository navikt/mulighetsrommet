import { BodyLong, Box } from "@navikt/ds-react";

interface Props {
  text: string;
  className?: string;
}

export default function Fritekstfelt({ text, className = "" }: Props) {
  return (
    <Box
      borderColor="border-subtle"
      padding="2"
      borderWidth="1"
      borderRadius="medium"
      className={`h-20 overflow-y-auto resize-y ${className}`}
      tabIndex={0}
      aria-label="Prisbetingelser"
    >
      <BodyLong className="whitespace-pre-line">{text}</BodyLong>
    </Box>
  );
}
