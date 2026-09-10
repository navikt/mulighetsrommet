import { Box } from "@navikt/ds-react";

interface Props {
  children: React.ReactNode;
}

export function KontaktpersonBox({ children }: Props) {
  return (
    <Box
      className="prose p-2 mt-2"
      borderColor="neutral-subtle"
      borderRadius="4"
      borderWidth="1"
      background="raised"
    >
      {children}
    </Box>
  );
}
