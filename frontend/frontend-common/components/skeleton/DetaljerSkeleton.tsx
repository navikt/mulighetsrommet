import { Skeleton, VStack } from "@navikt/ds-react";

export function DetaljerSkeleton() {
  return (
    <div
      style={{
        display: "grid",
        gridTemplateColumns: "65% 35%",
        gridTemplateRows: "auto 1fr",
        gap: "2rem",
      }}
    >
      <div>
        <VStack gap="2">
          <Skeleton height={200} variant="rounded" />
          <Skeleton height={500} variant="rounded" />
        </VStack>
      </div>

      <div>
        <VStack gap="2">
          <Skeleton height={600} variant="rounded" />
          <Skeleton height={100} variant="rounded" />
        </VStack>
      </div>
    </div>
  );
}
