import { Id } from './Generic';

export interface Tiltaksvariant {
  id?: Id;
  innsatsgruppe: number | null;
  tittel: string;
  ingress: string;
  beskrivelse: string;
}
