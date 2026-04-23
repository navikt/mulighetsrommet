import { z } from "zod";
import { SortDirection, TiltakstypeSortField } from "@tiltaksadministrasjon/api-client";
import { createFilterValidator } from "@/filter/filter-validator";
import { createFilterStateAtom } from "@/filter/filter-state";

const TiltakstypeFilterSchema = z.object({
  sort: z.object({
    field: z.enum(TiltakstypeSortField),
    direction: z.enum(SortDirection),
  }),
});

export type TiltakstypeFilterType = z.infer<typeof TiltakstypeFilterSchema>;

export const defaultTiltakstypeFilter: TiltakstypeFilterType = {
  sort: {
    field: TiltakstypeSortField.NAVN,
    direction: SortDirection.ASC,
  },
};

export const tiltakstypeFilterStateAtom = createFilterStateAtom<TiltakstypeFilterType>(
  "tiltakstype-filter",
  defaultTiltakstypeFilter,
  createFilterValidator(TiltakstypeFilterSchema),
);
