import { z } from "zod";
import type { TilsagnStatus } from "@tiltaksadministrasjon/api-client";
import { createFilterStateAtom } from "@/filter/filter-state";
import {
  createFilterValidator,
  createGracefulParser,
} from "@mr/frontend-common/utils/filter-validator";
import { createSorteringProps } from "@/api/atoms";
import { atom } from "jotai/vanilla";

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

export const tilsagnFilterStateAtom = createFilterStateAtom<TilsagnFilterType>(
  "Tilsagn-filter",
  defaultTilsagnFilter,
  createFilterValidator(TilsagnFilterSchema),
);

export const parseTilsagnFilter = createGracefulParser(TilsagnFilterSchema, defaultTilsagnFilter);

export const tilsagnFilterAccordionAtom = atom<string[]>(["lagrede-filter"]);
