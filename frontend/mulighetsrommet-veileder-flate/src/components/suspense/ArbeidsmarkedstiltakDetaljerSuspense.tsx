import React, { PropsWithChildren } from "react";
import { DetaljerSkeleton } from "@mr/frontend-common";

export function ArbeidsmarkedstiltakDetaljerSuspense(props: PropsWithChildren) {
  return <React.Suspense fallback={<DetaljerSkeleton />}>{props.children}</React.Suspense>;
}
