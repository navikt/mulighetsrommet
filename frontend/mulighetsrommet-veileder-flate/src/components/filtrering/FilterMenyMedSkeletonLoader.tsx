import { Suspense } from "react";
import { FilterSkeleton } from "mulighetsrommet-frontend-common";
import { Filtermeny } from "@/components/filtrering/Filtermeny";

export function FilterMenyMedSkeletonLoader() {
  return (
    <Suspense fallback={<FilterSkeleton />}>
      <Filtermeny />
    </Suspense>
  );
}
