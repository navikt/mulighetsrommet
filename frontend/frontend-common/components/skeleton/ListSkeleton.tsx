import { Skeleton, VStack } from "@navikt/ds-react";

export function ListSkeleton() {
  return (
      <div>
        <VStack style={{ margin: ".5rem" }} gap="2">
          <Skeleton height={60} variant="rounded" />
          <Skeleton height={60} variant="rounded" />
          <Skeleton height={60} variant="rounded" />
          <Skeleton height={60} variant="rounded" />
          <Skeleton height={60} variant="rounded" />
        </VStack>
      </div>
  );
}
