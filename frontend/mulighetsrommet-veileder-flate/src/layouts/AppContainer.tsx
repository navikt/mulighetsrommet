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
    <div className={` bg-bg-subtle min-h-dvh`}>
      {header}
      <ErrorBoundary FallbackComponent={ErrorFallback}>
        <React.Suspense fallback={<TiltakLoader />}>
          <div className={`max-w-[1920px] my-0 mx-auto scroll-smooth w-full py-3 px-2`}>
            {children}
          </div>
        </React.Suspense>
      </ErrorBoundary>
    </div>
  );
}
