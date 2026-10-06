import {
  createFilterValidator,
  createGracefulParser,
} from "@mr/frontend-common/utils/filter-validator";
import { PAGE_SIZE } from "@/constants";
import { Tiltakskode } from "@tiltaksadministrasjon/api-client";
import { z } from "zod";
import { createFilterStateAtom } from "@/filter/filter-state";
import { atom } from "jotai";
import { createSorteringProps } from "@/api/atoms";

export const TiltakBeskrivelseFilterSchema = z.object({
  navEnheter: z.string().array(),
  tiltakstyper: z.custom<Tiltakskode>().array(),
  sortering: createSorteringProps(z.string()),
  publisert: z.string().array(),
  visMineTiltakBeskrivelser: z.boolean(),
  page: z.number(),
  pageSize: z.number(),
});

export type TiltakBeskrivelseFilterType = z.infer<typeof TiltakBeskrivelseFilterSchema>;

export const defaultTiltakBeskrivelseFilter: TiltakBeskrivelseFilterType = {
  navEnheter: [],
  tiltakstyper: [],
  sortering: {
    sortString: "navn-ascending",
    tableSort: {
      orderBy: "navn",
      direction: "ascending",
    },
  },
  publisert: [],
  visMineTiltakBeskrivelser: false,
  page: 1,
  pageSize: PAGE_SIZE,
};

export const tiltakBeskrivelseFilterStateAtom = createFilterStateAtom<TiltakBeskrivelseFilterType>(
  "tiltak-beskrivelse-filter",
  defaultTiltakBeskrivelseFilter,
  createFilterValidator(TiltakBeskrivelseFilterSchema),
);

export const parseTiltakBeskrivelseFilter = createGracefulParser(
  TiltakBeskrivelseFilterSchema,
  defaultTiltakBeskrivelseFilter,
);

export const tiltakBeskrivelseFilterAccordionAtom = atom<string[]>(["navEnhet", "tiltakstype"]);
