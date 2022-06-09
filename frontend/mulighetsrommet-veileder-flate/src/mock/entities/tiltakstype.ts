import { Entity } from '@mswjs/data/lib/glossary';
import { Tiltakstype } from 'mulighetsrommet-api-client';
import { DatabaseDictionary } from '../database';

export type TiltakstypeEntity = Entity<DatabaseDictionary, 'tiltakstype'>;

export function toTiltakstype(entity: TiltakstypeEntity): Tiltakstype {
  return {
    _id: entity.id,
    innsatsgruppe: entity.innsatsgruppe!!,
    tiltakstypeNavn: entity.tiltakstypeNavn,
  };
}
