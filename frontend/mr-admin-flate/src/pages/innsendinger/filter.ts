import { atom } from "jotai";
import { z } from "zod";
import { createFilterStateAtom } from "@/filter/filter-state";
import { createFilterValidator } from "@/filter/filter-validator";

export const InnsendingFilterSchema = z.object({
  tiltakstyper: z.string().array(),
  regioner: z.array(z.string()),
});

export type InnsendingFilterType = z.infer<typeof InnsendingFilterSchema>;

const defaultInnsendingFilter: InnsendingFilterType = {
  tiltakstyper: [],
  regioner: [],
};

export const InnsendingFilterStateAtom = createFilterStateAtom<InnsendingFilterType>(
  "Innsending-filter",
  defaultInnsendingFilter,
  createFilterValidator(InnsendingFilterSchema),
);

export const InnsendingFilterAccordionAtom = atom<string[]>(["type", "regioner"]);
