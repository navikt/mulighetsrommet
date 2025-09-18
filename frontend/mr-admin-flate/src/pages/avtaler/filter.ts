import { AVTALE_PAGE_SIZE } from "@/constants";
import { z } from "zod";
import { createSorteringProps } from "@/api/atoms";
import { createFilterValidator } from "@/filter/filter-validator";
import { createFilterStateAtom, FilterAction, FilterState } from "@/filter/filter-state";
import { atomFamily } from "jotai/utils";
import { atom, WritableAtom } from "jotai";
import { AvtaleStatusType, Avtaletype } from "@tiltaksadministrasjon/api-client";

export const AvtaleFilterSchema = z.object({
  sok: z.string(),
  statuser: z.custom<AvtaleStatusType>().array(),
  avtaletyper: z.custom<Avtaletype>().array(),
  navRegioner: z.string().array(),
  tiltakstyper: z.string().array(),
  sortering: createSorteringProps(z.string()),
  arrangorer: z.string().array(),
  visMineAvtaler: z.boolean(),
  personvernBekreftet: z.boolean().optional(),
  page: z.number(),
  pageSize: z.number(),
});

export type AvtaleFilterType = z.infer<typeof AvtaleFilterSchema>;

export const defaultAvtaleFilter: AvtaleFilterType = {
  sok: "",
  statuser: [AvtaleStatusType.AKTIV],
  avtaletyper: [],
  navRegioner: [],
  tiltakstyper: [],
  sortering: {
    sortString: "navn-ascending",
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
  return avtalerForTiltakstypeFilterAtomFamily(defaultFilterValue);
}

const avtalerForTiltakstypeFilterAtomFamily = atomFamily<
  AvtaleFilterType,
  WritableAtom<FilterState<AvtaleFilterType>, [FilterAction<AvtaleFilterType>], void>
>(
  (defaultFilter: AvtaleFilterType) => {
    return createFilterStateAtom(
      `avtale-filter-${defaultFilter.tiltakstyper[0]}`,
      defaultFilter,
      createFilterValidator(AvtaleFilterSchema),
    );
  },
  (a, b) => a.tiltakstyper[0] === b.tiltakstyper[0],
);

export const avtaleFilterAccordionAtom = atom<string[]>(["region"]);
