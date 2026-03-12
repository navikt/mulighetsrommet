import { HGrid } from "@navikt/ds-react";

export function MetadataContainer({ children }: { children: React.ReactNode }) {
  return (
    <HGrid columns="1fr 1fr" gap="space-24">
      {children}
    </HGrid>
  );
}
