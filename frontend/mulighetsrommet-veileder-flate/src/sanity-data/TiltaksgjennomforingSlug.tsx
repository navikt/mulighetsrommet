import React from 'react';
import { TiltakstypeI } from './TiltakstypeSlug';

export interface TiltaksgjennomforingI {
  _id: string;
  title: string;
  tiltakstype: TiltakstypeI;
  slug: string;
  tiltaksnummer: number;
  leverandor: string;
  oppstartsdato?: Date;
  faneinnhold: {
    forhvem: string;
    detaljeroginnhold: string;
    pameldingogvarighet: string;
    kontakinfofagansvarlig: { kontaktinfoleverandor: string; kontaktinfotiltaksansvarlig: string };
  };
}
