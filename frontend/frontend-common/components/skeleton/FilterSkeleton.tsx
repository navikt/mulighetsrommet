import { Skeleton, VStack } from "@navikt/ds-react";

export function FilterSkeleton() {
  return (
    <VStack gap="space-8">
      <Skeleton height={50} variant="rounded" />
      <Skeleton height={200} variant="rounded" />
      <Skeleton height={50} variant="rounded" />
      <Skeleton height={50} variant="rounded" />
      <Skeleton height={50} variant="rounded" />
      <Skeleton height={50} variant="rounded" />
      <Skeleton height={50} variant="rounded" />
    </VStack>
  );
}
