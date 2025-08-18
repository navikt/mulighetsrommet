import { BodyLong, Box } from "@navikt/ds-react";

export default function Prisbetingelser({ value }: { value: string }) {
  return (
    <Box
      borderColor="border-subtle"
      padding="2"
      borderWidth="1"
      borderRadius="medium"
      className="max-h-[200px] overflow-y-auto"
      tabIndex={0}
      aria-label="Prisbetingelser"
    >
      <BodyLong className="whitespace-pre-line">{value}</BodyLong>
    </Box>
  );
}
