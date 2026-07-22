import { createFilterValidator, createGracefulParser } from "@/filter/filter-validator";
import { PAGE_SIZE } from "@/constants";
import { Tiltakskode } from "@tiltaksadministrasjon/api-client";
import { z } from "zod";
import { createFilterStateAtom } from "@/filter/filter-state";
import { atom } from "jotai";
import { createSorteringProps } from "@/api/atoms";

export const TiltakDokumentFilterSchema = z.object({
  navEnheter: z.string().array(),
  tiltakstyper: z.custom<Tiltakskode>().array(),
  sortering: createSorteringProps(z.string()),
  page: z.number(),
  pageSize: z.number(),
});

export type TiltakDokumentFilterType = z.infer<typeof TiltakDokumentFilterSchema>;

export const defaultTiltakDokumentFilter: TiltakDokumentFilterType = {
  navEnheter: [],
  tiltakstyper: [],
  sortering: {
    sortString: "navn-ascending",
    tableSort: {
      orderBy: "navn",
      direction: "ascending",
    },
  },
  page: 1,
  pageSize: PAGE_SIZE,
};

export const tiltakDokumentFilterStateAtom = createFilterStateAtom<TiltakDokumentFilterType>(
  "tiltak-dokument-filter",
  defaultTiltakDokumentFilter,
  createFilterValidator(TiltakDokumentFilterSchema),
);

export const parseTiltakDokumentFilter = createGracefulParser(
  TiltakDokumentFilterSchema,
  defaultTiltakDokumentFilter,
);

export const tiltakDokumentFilterAccordionAtom = atom<string[]>(["navEnhet", "tiltakstype"]);
