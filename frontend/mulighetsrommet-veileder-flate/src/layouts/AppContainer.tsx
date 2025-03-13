import React, { ReactNode } from "react";
import { ErrorFallback } from "@/utils/ErrorFallback";
import { ErrorBoundary } from "react-error-boundary";
import { TiltakLoader } from "@/components/TiltakLoader";

interface Props {
  header?: ReactNode;
  children: ReactNode;
}

export function AppContainer({ children, header }: Props) {
  return (
    <div className="min-h-[100dvh] bg-[var(--a-bg-subtle)]">
      {header}
      <ErrorBoundary FallbackComponent={ErrorFallback}>
        <React.Suspense fallback={<TiltakLoader />}>
          <div className="mx-auto w-full max-w-[1920px] scroll-smooth px-2 py-3">{children}</div>
        </React.Suspense>
      </ErrorBoundary>
    </div>
  );
}
