import { AVTALE_PAGE_SIZE } from "@/constants";
import { Avtalestatus, Avtaletype, SorteringAvtaler } from "@mr/api-client-v2";
import { z } from "zod";
import { createFilterValidator, createSorteringProps } from "@/api/atoms";
import { createFilterStateAtom } from "@/filter/filter-state";
import { atomFamily, atomWithStorage, createJSONStorage } from "jotai/utils";
import { atom, WritableAtom } from "jotai";

export const AvtaleFilterSchema = z.object({
  sok: z.string(),
  statuser: z.custom<Avtalestatus>().array(),
  avtaletyper: z.custom<Avtaletype>().array(),
  navRegioner: z.string().array(),
  tiltakstyper: z.string().array(),
  sortering: createSorteringProps(z.custom<SorteringAvtaler>()),
  arrangorer: z.string().array(),
  visMineAvtaler: z.boolean(),
  personvernBekreftet: z.boolean().optional(),
  page: z.number(),
  pageSize: z.number(),
});

export type AvtaleFilterType = z.infer<typeof AvtaleFilterSchema>;

export const defaultAvtaleFilter: AvtaleFilterType = {
  sok: "",
  statuser: [Avtalestatus.AKTIV],
  avtaletyper: [],
  navRegioner: [],
  tiltakstyper: [],
  sortering: {
    sortString: SorteringAvtaler.NAVN_ASCENDING,
    tableSort: {
      orderBy: "navn",
      direction: "ascending",
    },
  },
  arrangorer: [],
  visMineAvtaler: false,
  personvernBekreftet: undefined,
  page: 1,
  pageSize: AVTALE_PAGE_SIZE,
};

export const avtalerFilterStateAtom = createFilterStateAtom<AvtaleFilterType>(
  "avtale-filter",
  defaultAvtaleFilter,
  createFilterValidator(AvtaleFilterSchema),
);

export function getAvtalerForTiltakstypeFilterAtom(tiltakstypeId: string) {
  const defaultFilterValue = { ...defaultAvtaleFilter, tiltakstyper: [tiltakstypeId] };
  const filterAtom = avtalerForTiltakstypeFilterAtomFamily(defaultFilterValue);
  return { defaultFilterValue, filterAtom };
}

const avtalerForTiltakstypeFilterAtomFamily = atomFamily<
  AvtaleFilterType,
  WritableAtom<AvtaleFilterType, [newValue: AvtaleFilterType], void>
>(
  (filter: AvtaleFilterType) => {
    return atomWithStorage(
      `avtale-filter-${filter.tiltakstyper[0]}`,
      filter,
      createJSONStorage(() => sessionStorage),
    );
  },
  (a, b) => a.tiltakstyper[0] === b.tiltakstyper[0],
);

export const avtaleFilterAccordionAtom = atom<string[]>(["region"]);
