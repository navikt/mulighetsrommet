/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { Innsatsgruppe } from  './Innsatsgruppe';
import { Enhet } from './Enhet';

export type Bruker = {
  fnr: string;
  innsatsgruppe: Innsatsgruppe;
  oppfolgingsenhet: Enhet;
};
