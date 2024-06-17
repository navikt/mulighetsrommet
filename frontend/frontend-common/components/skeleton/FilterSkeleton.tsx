import { Skeleton, VStack } from "@navikt/ds-react";

export function FilterSkeleton() {
  return (
    <VStack gap="2">
      <Skeleton height={50} variant="rounded" />
      <Skeleton height={200} variant="rounded" />
      <Skeleton height={50} variant="rounded" />
      <Skeleton height={100} variant="rounded" />
      <Skeleton height={50} variant="rounded" />
    </VStack>
  );
}
