import { ARRANGORER_PAGE_SIZE } from "@/constants";
import { z } from "zod";
import { createSorteringProps } from "@/api/atoms";
import { createFilterStateAtom } from "@/filter/filter-state";
import { createFilterValidator } from "@/filter/filter-validator";

const ArrangorerFilterSchema = z.object({
  sok: z.string(),
  page: z.number(),
  pageSize: z.number(),
  sortering: createSorteringProps(z.string()),
});

export type ArrangorerFilterType = z.infer<typeof ArrangorerFilterSchema>;

const defaultArrangorerFilter: ArrangorerFilterType = {
  sok: "",
  sortering: {
    sortString: "navn-ascending",
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
