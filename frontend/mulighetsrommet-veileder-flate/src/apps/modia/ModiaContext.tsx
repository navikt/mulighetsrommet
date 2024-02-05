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
  const [contextData, setContextData] = useAtom(modiaContextAtom);

  const [loadedFilter, setLoadedFilter] = useState<FilterMedBrukerIKontekst | null>(null);

  useEffect(() => {
    if (props.contextData) {
      props?.updateContextDataRef?.((key: string, value: string) => {
        setContextData({ ...contextData, [key]: value });
      });
    }
  }, [props.contextData.enhet, props.contextData.fnr]);

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
