import { Skeleton, VStack } from "@navikt/ds-react";

export function OversiktSkeleton() {
  return (
    <div
      style={{
        display: "grid",
        gridTemplateColumns: "var(--filter-width) auto",
        gridTemplateRows: "3rem auto",
        columnGap: ".5rem",
      }}
    >
      <div>
        <Skeleton height={600} variant="rounded" />
      </div>

      <div>
        <VStack gap="2">
          <Skeleton height={40} variant="rounded" />
          <Skeleton height={50} variant="rounded" />
          <Skeleton height={30} variant="rounded" />
          <Skeleton height={600} variant="rounded" />
        </VStack>
      </div>
    </div>
  );
}
