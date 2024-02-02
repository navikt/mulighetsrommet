import { Skeleton, VStack } from "@navikt/ds-react";
import { Suspense } from "react";
import { Filtermeny } from "./Filtermeny";

function FilterSkeletonLoader() {
  return (
    <VStack gap="2">
      <Skeleton height={50} variant="rounded" />
      <Skeleton height={50} variant="rounded" />
      <Skeleton height={200} variant="rounded" />
      <Skeleton height={110} variant="rounded" />
      <Skeleton height={110} variant="rounded" />
    </VStack>
  );
}

export function FilterMenyWithSkeletonLoader() {
  return (
    <Suspense fallback={<FilterSkeletonLoader />}>
      <Filtermeny />
    </Suspense>
  );
}
