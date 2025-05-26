import { createFilterValidator, createSorteringProps } from "@/api/atoms";
import { PAGE_SIZE } from "@/constants";
import { GjennomforingStatus, NavEnhet, SorteringGjennomforinger } from "@mr/api-client-v2";
import { z } from "zod";
import { createFilterStateAtom } from "@/filter/filter-state";
import { atom, WritableAtom } from "jotai";
import { atomFamily, atomWithStorage, createJSONStorage } from "jotai/utils";

export const GjennomforingFilterSchema = z.object({
  search: z.string(),
  navEnheter: z.custom<NavEnhet>().array(),
  tiltakstyper: z.string().array(),
  statuser: z.custom<GjennomforingStatus>().array(),
  sortering: createSorteringProps(z.custom<SorteringGjennomforinger>()),
  avtale: z.string(),
  arrangorer: z.string().array(),
  visMineGjennomforinger: z.boolean(),
  publisert: z.string().array(),
  page: z.number(),
  pageSize: z.number(),
});

export type GjennomforingFilterType = z.infer<typeof GjennomforingFilterSchema>;

export const defaultGjennomforingFilter: GjennomforingFilterType = {
  search: "",
  navEnheter: [],
  tiltakstyper: [],
  statuser: [GjennomforingStatus.GJENNOMFORES],
  sortering: {
    sortString: SorteringGjennomforinger.NAVN_ASCENDING,
    tableSort: {
      orderBy: "navn",
      direction: "ascending",
    },
  },
  avtale: "",
  arrangorer: [],
  publisert: [],
  visMineGjennomforinger: false,
  page: 1,
  pageSize: PAGE_SIZE,
};

export const gjennomforingFilterStateAtom = createFilterStateAtom<GjennomforingFilterType>(
  "gjennomforing-filter",
  defaultGjennomforingFilter,
  createFilterValidator(GjennomforingFilterSchema),
);

export function getGjennomforingerForAvtaleFilterAtom(avtaleId: string) {
  const defaultFilterValue = { ...defaultGjennomforingFilter, avtale: avtaleId };
  const filterAtom = gjennomforingerForAvtaleFilterAtomFamily(defaultFilterValue);
  return { defaultFilterValue, filterAtom };
}

const gjennomforingerForAvtaleFilterAtomFamily = atomFamily<
  GjennomforingFilterType,
  WritableAtom<GjennomforingFilterType, [newValue: GjennomforingFilterType], void>
>(
  (filter: GjennomforingFilterType) => {
    return atomWithStorage(
      `gjennomforing-filter-${filter.avtale}`,
      filter,
      createJSONStorage(() => sessionStorage),
    );
  },
  (a, b) => a.avtale === b.avtale,
);

export const gjennomforingFilterAccordionAtom = atom<string[]>(["navEnhet"]);
