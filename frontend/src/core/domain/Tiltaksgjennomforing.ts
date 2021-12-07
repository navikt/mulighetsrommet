import { Id } from './Generic';

export interface Tiltaksgjennomforing {
  id?: Id;
  tiltaksvariantId: Id;
  tiltaksnummer: Id;
  tittel: string;
  beskrivelse: string;
  fraDato: Date;
  tilDato: Date;
}
