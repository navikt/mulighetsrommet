import { SorteringTiltakstyper } from "@mr/api-client-v2";
import { z } from "zod";
import { createSorteringProps } from "@/api/atoms";
import { createFilterValidator } from "@/filter/filter-validator";
import { createFilterStateAtom } from "@/filter/filter-state";

const TiltakstypeFilterSchema = z.object({
  sort: createSorteringProps(z.custom<SorteringTiltakstyper>()).optional(),
});

export type TiltakstypeFilterType = z.infer<typeof TiltakstypeFilterSchema>;

const defaultTiltakstypeFilter: TiltakstypeFilterType = {
  sort: {
    sortString: SorteringTiltakstyper.NAVN_ASCENDING,
    tableSort: {
      orderBy: "navn",
      direction: "ascending",
    },
  },
};

export const tiltakstypeFilterStateAtom = createFilterStateAtom<TiltakstypeFilterType>(
  "tiltakstype-filter",
  defaultTiltakstypeFilter,
  createFilterValidator(TiltakstypeFilterSchema),
);
