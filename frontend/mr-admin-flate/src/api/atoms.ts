import { SortState } from "@navikt/ds-react";
import { atom } from "jotai";
import {
  atomWithStorage,
  createJSONStorage,
  unstable_withStorageValidator as withStorageValidator,
} from "jotai/utils";
import { OppgaveType } from "@mr/api-client-v2";
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

const OppgaverFilterSchema = z.object({
  type: z.nativeEnum(OppgaveType).array(),
  tiltakstyper: z.array(z.string()),
  regioner: z.array(z.string()),
});

export type OppgaverFilterType = z.infer<typeof OppgaverFilterSchema>;

export const defaultOppgaverFilter: OppgaverFilterType = {
  type: [],
  tiltakstyper: [],
  regioner: [],
};

const oppgaverFilterStorage = withStorageValidator(createFilterValidator(OppgaverFilterSchema))(
  createJSONStorage(() => sessionStorage),
);

export const oppgaverFilterAtom = atomWithStorage<OppgaverFilterType>(
  "oppgaver-filter",
  defaultOppgaverFilter,
  oppgaverFilterStorage,
  { getOnInit: true },
);

export const gjennomforingDetaljerTabAtom = atom<"detaljer" | "redaksjonelt-innhold">("detaljer");

export const avtaleDetaljerTabAtom = atom<
  "detaljer" | "okonomi" | "personvern" | "redaksjonelt-innhold"
>("detaljer");

export const oppgaverFilterAccordionAtom = atom<string[]>(["type", "regioner"]);
