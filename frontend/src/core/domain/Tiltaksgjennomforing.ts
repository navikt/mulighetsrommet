import { Id } from './Generic';

export interface Tiltaksgjennomforing {
  id?: Id;
  tiltaksvariantId: number;
  tiltaksnummer: string;
  tittel: string;
  beskrivelse: string;
  fraDato: Date;
  tilDato: Date;
}
