import { Id } from './Generic';

export interface Tiltaksvariant {
  id?: number;
  innsatsgruppe: number | null;
  tittel: string;
  ingress: string;
  beskrivelse: string;
}
