import { atom } from "jotai";
import { z } from "zod";
import { createFilterStateAtom } from "@/filter/filter-state";
import { createFilterValidator } from "@/filter/filter-validator";
import { createSorteringProps } from "@/api/atoms";

export const InnsendingFilterSchema = z.object({
  tiltakstyper: z.string().array(),
  kostnadssteder: z.string().array(),
  sortering: createSorteringProps(z.string()),
});

export type InnsendingFilterType = z.infer<typeof InnsendingFilterSchema>;

const defaultInnsendingFilter: InnsendingFilterType = {
  tiltakstyper: [],
  kostnadssteder: [],
  sortering: {
    sortString: "navn-ascending",
    tableSort: {
      orderBy: "navn",
      direction: "ascending",
    },
  },
};

export const InnsendingFilterStateAtom = createFilterStateAtom<InnsendingFilterType>(
  "Innsending-filter",
  defaultInnsendingFilter,
  createFilterValidator(InnsendingFilterSchema),
);

export const InnsendingFilterAccordionAtom = atom<string[]>(["tiltakstype", "navEnhet"]);
