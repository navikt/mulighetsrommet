import { SortState } from "@navikt/ds-react";
import { atom } from "jotai";
import { z } from "zod";

export function createSorteringProps<T>(sortItems: T) {
  return z.object({
    tableSort: z.custom<SortState>(),
    sortString: sortItems,
  });
}

export const gjennomforingDetaljerTabAtom = atom<
  "tilsagn" | "deltakerliste" | "utbetalinger" | "detaljer" | "redaksjonelt-innhold"
>("detaljer");

export const avtaleDetaljerTabAtom = atom<
  "detaljer" | "personvern" | "redaksjonelt-innhold" | "gjennomforinger"
>("detaljer");
