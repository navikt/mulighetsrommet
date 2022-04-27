/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { Tiltakskode } from './Tiltakskode';

export type Tiltakstype = {
    id: number;
    innsatsgruppe: number | null;
    sanityId: number | null;
    navn: string;
    tiltakskode: Tiltakskode;
    fraDato: string | null;
    tilDato: string | null;
};
