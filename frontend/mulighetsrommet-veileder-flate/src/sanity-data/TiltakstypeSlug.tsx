import React from 'react';
import { Innsatsgruppe } from '../../../mulighetsrommet-api-client';

export interface TiltakstypeI {
  _id: string;
  title: string;
  ingress: string;
  innsatsgruppe: Innsatsgruppe;
  oppstart: string;
  faneinnhold: {
    detaljeroginnhold: string;
    forhvem: string;
    pameldingogvarighet: string;
    kontakinfofagansvarlig: { fagansvarlig: string; telefonnummer: number; epost: string; adresse: string };
  };
}
