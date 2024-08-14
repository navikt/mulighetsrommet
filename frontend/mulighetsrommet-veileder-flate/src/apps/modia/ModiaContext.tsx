import { Provider as JotaiProvider, useAtom } from "jotai";
import { useHydrateAtoms } from "jotai/utils";
import { ReactNode, useEffect, useState } from "react";
import { AppContextData, modiaContextAtom } from "./hooks/useModiaContext";
import {
  filterAtom,
  FilterMedBrukerIKontekst,
  getDefaultFilterForBrukerIKontekst,
} from "@/hooks/useArbeidsmarkedstiltakFilter";
import { ReactQueryProvider } from "@/ReactQueryProvider";

export interface ModiaContextProps {
  contextData: Partial<AppContextData>;
  updateContextDataRef?: (updateContextData: (key: string, value: string) => void) => void;
  children: ReactNode;
}

function HydrateAtoms({
  appContext,
  filter,
  children,
}: {
  appContext: Partial<AppContextData>;
  filter: FilterMedBrukerIKontekst;
  children: ReactNode;
}) {
  /**
   * Initialiserer atoms som trenger standardverdier basert på bruker i kontekst
   */
  useHydrateAtoms([
    [modiaContextAtom, appContext],
    [filterAtom, filter],
  ]);
  return children;
}

export function ModiaContext(props: ModiaContextProps) {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const [_, setContextData] = useAtom(modiaContextAtom);

  const [loadedFilter, setLoadedFilter] = useState<FilterMedBrukerIKontekst | null>(null);

  const { contextData: contextDataFraProps, updateContextDataRef } = props;
  useEffect(() => {
    if (contextDataFraProps) {
      updateContextDataRef?.((key: string, value: string) => {
        setContextData({ ...contextDataFraProps, [key]: value });
      });
    }
  }, [contextDataFraProps, updateContextDataRef, setContextData]);

  useEffect(() => {
    const filter = getDefaultFilterForBrukerIKontekst(props.contextData.fnr ?? null);
    setLoadedFilter(filter);
  }, [props.contextData.fnr]);

  if (!loadedFilter) {
    return null;
  }

  return (
    <ReactQueryProvider>
      <JotaiProvider>
        <HydrateAtoms appContext={props.contextData} filter={loadedFilter}>
          {props.children}
        </HydrateAtoms>
      </JotaiProvider>
    </ReactQueryProvider>
  );
}
