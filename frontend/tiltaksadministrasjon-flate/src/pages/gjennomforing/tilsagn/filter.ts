import { z } from "zod";
import type { TilsagnStatus } from "@tiltaksadministrasjon/api-client";
import { createFilterStateAtom } from "@/filter/filter-state";
import { createFilterValidator } from "@mr/frontend-common/utils/filter-validator";
import { createSorteringProps } from "@/api/atoms";
import { atom } from "jotai/vanilla";
import { atomFamily } from "jotai-family";

export const TilsagnFilterSchema = z.object({
  statuser: z.array(z.custom<TilsagnStatus>()),
  kostnadssteder: z.array(z.string()),
  sortering: createSorteringProps(z.string()),
  page: z.number(),
  pageSize: z.number(),
});

export type TilsagnFilterType = z.infer<typeof TilsagnFilterSchema>;

const defaultTilsagnFilter: TilsagnFilterType = {
  statuser: [],
  kostnadssteder: [],
  sortering: {
    sortString: "navn-ascending",
    tableSort: {
      orderBy: "navn",
      direction: "ascending",
    },
  },
  page: 1,
  pageSize: 15,
};

export const tilsagnFilterStateAtomFamily = atomFamily((gjennomforingId: string) =>
  createFilterStateAtom<TilsagnFilterType>(
    `Tilsagn-filter-${gjennomforingId}`,
    defaultTilsagnFilter,
    createFilterValidator(TilsagnFilterSchema),
  ),
);

export const tilsagnFilterAccordionAtom = atom<string[]>(["status", "kostnadssted"]);
