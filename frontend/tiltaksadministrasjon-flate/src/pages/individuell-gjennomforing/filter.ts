import { createFilterValidator, createGracefulParser } from "@/filter/filter-validator";
import { PAGE_SIZE } from "@/constants";
import { Tiltakskode } from "@tiltaksadministrasjon/api-client";
import { z } from "zod";
import { createFilterStateAtom } from "@/filter/filter-state";
import { atom } from "jotai";

export const IndividuellGjennomforingFilterSchema = z.object({
  navEnheter: z.string().array(),
  tiltakstyper: z.custom<Tiltakskode>().array(),
  page: z.number(),
  pageSize: z.number(),
});

export type IndividuellGjennomforingFilterType = z.infer<
  typeof IndividuellGjennomforingFilterSchema
>;

export const defaultIndividuellGjennomforingFilter: IndividuellGjennomforingFilterType = {
  navEnheter: [],
  tiltakstyper: [],
  page: 1,
  pageSize: PAGE_SIZE,
};

export const individuellGjennomforingFilterStateAtom =
  createFilterStateAtom<IndividuellGjennomforingFilterType>(
    "individuell-gjennomforing-filter",
    defaultIndividuellGjennomforingFilter,
    createFilterValidator(IndividuellGjennomforingFilterSchema),
  );

export const parseIndividuellGjennomforingFilter = createGracefulParser(
  IndividuellGjennomforingFilterSchema,
  defaultIndividuellGjennomforingFilter,
);

export const individuellGjennomforingFilterAccordionAtom = atom<string[]>([
  "navEnhet",
  "tiltakstype",
]);
