import { atom } from "jotai";
import { z } from "zod";
import { createFilterStateAtom } from "@/filter/filter-state";
import { createFilterValidator } from "@/filter/filter-validator";

export const InnsendingFilterSchema = z.object({
  tiltakstyper: z.string().array(),
  periode: z.object({
    start: z.string().min(1),
    slutt: z.string().min(1),
  }),
  regioner: z.array(z.string()),
});

export type InnsendingFilterType = z.infer<typeof InnsendingFilterSchema>;

const defaultInnsendingFilter: InnsendingFilterType = {
  tiltakstyper: [],
  periode: {
    start: "2025-10-01",
    slutt: "2025-10-31",
  },
  regioner: [],
};

export const InnsendingFilterStateAtom = createFilterStateAtom<InnsendingFilterType>(
  "Innsending-filter",
  defaultInnsendingFilter,
  createFilterValidator(InnsendingFilterSchema),
);

export const InnsendingFilterAccordionAtom = atom<string[]>(["periode", "type", "regioner"]);
