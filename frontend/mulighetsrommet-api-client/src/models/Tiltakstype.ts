/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { Tiltakskode } from "./Tiltakskode";

export type Tiltakstype = {
  id: number;
  innsatsgruppe: number | null;
  sanityId: number | null;
  navn: string;
  tiltakskode: Tiltakskode;
  fraDato: Date | null;
  tilDato: Date | null;
  createdBy: string | null;
  createdAt: string | null;
  updatedBy: string | null;
  updatedAt: string | null;
};
