import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { Provider, useAtom } from "jotai";
import { useHydrateAtoms } from "jotai/utils";
import { ReactNode, useEffect, useState } from "react";
import { appContext } from "./core/atoms/atoms";
import { AppContextData } from "./hooks/useAppContext";
import {
  filterAtom,
  FilterMedBrukerIKontekst,
  getDefaultFilterForBrukerIKontekst,
} from "./hooks/useArbeidsmarkedstiltakFilter";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      throwOnError: true,
      retry: 3,
    },
  },
});

export interface AppContextProps {
  contextData: Partial<AppContextData>;
  updateContextDataRef?: (updateContextData: (key: string, value: string) => void) => void;
  children: ReactNode;
}

function HydrateAtoms({
  contextData,
  filter,
  children,
}: {
  contextData: Partial<AppContextData>;
  filter: FilterMedBrukerIKontekst;
  children: ReactNode;
}) {
  /**
   * Initialiserer atoms som trenger standardverdier basert på bruker i kontekst
   */
  useHydrateAtoms([
    [appContext, contextData],
    [filterAtom, filter],
  ]);
  return children;
}

export function AppContext(props: AppContextProps) {
  const [contextData, setContextData] = useAtom(appContext);

  const [loadedFilter, setLoadedFilter] = useState<FilterMedBrukerIKontekst | null>(null);

  useEffect(() => {
    if (props.contextData) {
      props?.updateContextDataRef?.((key: string, value: string) => {
        setContextData({ ...contextData, [key]: value });
      });
    }
  }, [props.contextData.enhet, props.contextData.fnr]);

  useEffect(() => {
    const filter = props.contextData.fnr
      ? getDefaultFilterForBrukerIKontekst(props.contextData.fnr)
      : null;
    setLoadedFilter(filter);
  }, [props.contextData.fnr]);

  if (!loadedFilter) {
    return null;
  }

  return (
    <QueryClientProvider client={queryClient}>
      <Provider>
        <HydrateAtoms contextData={props.contextData} filter={loadedFilter}>
          {props.children}
        </HydrateAtoms>
      </Provider>
      <ReactQueryDevtools initialIsOpen={false} />
    </QueryClientProvider>
  );
}
