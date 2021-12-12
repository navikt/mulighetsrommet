import { Entity } from '@mswjs/data/lib/glossary';
import { Tiltaksvariant } from '../../api';
import { DatabaseDictionary } from '../database';

export type TiltaksvariantEntity = Entity<DatabaseDictionary, 'tiltaksvariant'>;

export function toTiltaksvariant(entity: TiltaksvariantEntity): Tiltaksvariant {
  return {
    id: entity.id,
    innsatsgruppe: entity.innsatsgruppe?.id ?? null,
    tittel: entity.tittel,
    ingress: entity.ingress,
    beskrivelse: entity.beskrivelse,
  };
}
