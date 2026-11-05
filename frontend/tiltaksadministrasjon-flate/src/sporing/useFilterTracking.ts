import { useCallback } from "react";
import { useUmami, type FilterEndring } from "./useUmami";

export function useFilterTracking(kontekst?: string) {
  const { logUmamiHendelse } = useUmami();

  const logFilterEndret = useCallback(
    (endring: FilterEndring) => {
      if (kontekst === undefined) return;
      logUmamiHendelse({ ...endring, type: "FILTER_ENDRET", kontekst });
    },
    [kontekst, logUmamiHendelse],
  );

  const logAccordionEndret = useCallback(
    (tittel: string, open: boolean) => {
      if (kontekst === undefined) return;
      logUmamiHendelse({
        type: open ? "ACCORDION_ÅPNET" : "ACCORDION_LUKKET",
        tittel,
        kontekst,
      });
    },
    [kontekst, logUmamiHendelse],
  );

  const logFilterpanelÅpnet = useCallback(() => {
    if (kontekst === undefined) return;
    logUmamiHendelse({
      type: "FILTERPANEL_ÅPNET",
      kontekst,
    });
  }, [kontekst, logUmamiHendelse]);

  return { logFilterEndret, logAccordionEndret, logFilterpanelÅpnet };
}
