import { SortState } from "@navikt/ds-react";
import { atom } from "jotai";
import { z, ZodType } from "zod";

export function createSorteringProps(sortItems: z.ZodType) {
  return z.object({
    tableSort: z.custom<SortState>(),
    sortString: sortItems,
  });
}

export function createFilterValidator<T>(schema: ZodType<T>) {
  return (values: unknown): values is T => {
    return Boolean(schema.safeParse(values).success);
  };
}

export const gjennomforingDetaljerTabAtom = atom<"detaljer" | "redaksjonelt-innhold">("detaljer");

export const avtaleDetaljerTabAtom = atom<
  "detaljer" | "okonomi" | "personvern" | "redaksjonelt-innhold"
>("detaljer");
