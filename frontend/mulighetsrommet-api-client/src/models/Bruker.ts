/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { Innsatsgruppe } from './Innsatsgruppe';
import type { Oppfolgingsenhet } from './Oppfolgingsenhet';

export type Bruker = {
    fnr: string;
    innsatsgruppe?: Innsatsgruppe;
    oppfolgingsenhet: Oppfolgingsenhet;
    fornavn?: string;
};
