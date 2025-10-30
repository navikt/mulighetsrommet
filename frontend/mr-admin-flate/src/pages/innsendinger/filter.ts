import { atom } from "jotai";
import { z } from "zod";
import { createFilterStateAtom } from "@/filter/filter-state";
import { createFilterValidator } from "@/filter/filter-validator";
import { NavEnhetDto } from "@tiltaksadministrasjon/api-client";
import { createSorteringProps } from "@/api/atoms";

export const InnsendingFilterSchema = z.object({
  tiltakstyper: z.string().array(),
  navEnheter: z.custom<NavEnhetDto>().array(),
  sortering: createSorteringProps(z.string()),
});

export type InnsendingFilterType = z.infer<typeof InnsendingFilterSchema>;

const defaultInnsendingFilter: InnsendingFilterType = {
  tiltakstyper: [],
  navEnheter: [],
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

export const InnsendingFilterAccordionAtom = atom<string[]>(["tiltakstype", "navEnheter"]);
