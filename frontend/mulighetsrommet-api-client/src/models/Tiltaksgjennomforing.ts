/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { Tiltakskode } from './Tiltakskode';

export type Tiltaksgjennomforing = {
    id: number;
    tittel: string;
    beskrivelse: string;
    tiltakskode: Tiltakskode;
    tiltaksnummer?: string;
    fraDato: string | null;
    tilDato: string | null;
};
