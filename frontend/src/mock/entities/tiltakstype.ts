import { Entity } from '@mswjs/data/lib/glossary';
import { Tiltakstype } from '../../api';
import { DatabaseDictionary } from '../database';

export type TiltakstypeEntity = Entity<DatabaseDictionary, 'tiltakstype'>;

export function toTiltakstype(entity: TiltakstypeEntity): Tiltakstype {
  return {
    id: entity.id,
    innsatsgruppe: entity.innsatsgruppe?.id ?? null,
    tittel: entity.tittel,
    ingress: entity.ingress,
    beskrivelse: entity.beskrivelse,
    arrangor: entity.arrangor,
  };
}
