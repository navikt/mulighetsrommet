import { atom } from "jotai";
import { OppgaveType, Tiltakskode } from "@mr/api-client-v2";
import { z } from "zod";
import { createFilterStateAtom } from "@/filter/filter-state";
import { createFilterValidator } from "@/filter/filter-validator";

export const OppgaverFilterSchema = z.object({
  type: z.nativeEnum(OppgaveType).array(),
  tiltakstyper: z.nativeEnum(Tiltakskode).array(),
  regioner: z.array(z.string()),
});

export type OppgaverFilterType = z.infer<typeof OppgaverFilterSchema>;

const defaultOppgaverFilter: OppgaverFilterType = {
  type: [],
  tiltakstyper: [],
  regioner: [],
};

export const oppgaverFilterStateAtom = createFilterStateAtom<OppgaverFilterType>(
  "oppgaver-filter",
  defaultOppgaverFilter,
  createFilterValidator(OppgaverFilterSchema),
);

export const oppgaverFilterAccordionAtom = atom<string[]>(["type", "regioner"]);
