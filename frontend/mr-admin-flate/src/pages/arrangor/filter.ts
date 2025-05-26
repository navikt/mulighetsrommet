import { ARRANGORER_PAGE_SIZE } from "@/constants";
import { SorteringArrangorer } from "@mr/api-client-v2";
import { z } from "zod";
import { createFilterValidator, createSorteringProps } from "@/api/atoms";
import { createFilterStateAtom } from "@/filter/filter-state";

const ArrangorerFilterSchema = z.object({
  sok: z.string(),
  page: z.number(),
  pageSize: z.number(),
  sortering: createSorteringProps(z.custom<SorteringArrangorer>()),
});

export type ArrangorerFilterType = z.infer<typeof ArrangorerFilterSchema>;

const defaultArrangorerFilter: ArrangorerFilterType = {
  sok: "",
  sortering: {
    sortString: SorteringArrangorer.NAVN_ASCENDING,
    tableSort: {
      orderBy: "navn",
      direction: "ascending",
    },
  },
  page: 1,
  pageSize: ARRANGORER_PAGE_SIZE,
};

export const arrangorerFilterStateAtom = createFilterStateAtom<ArrangorerFilterType>(
  "arrangorer-filter",
  defaultArrangorerFilter,
  createFilterValidator(ArrangorerFilterSchema),
);
