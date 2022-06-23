import { Entity } from '@mswjs/data/lib/glossary';
import { DatabaseDictionary } from '../database';
import { Tiltakstype } from '../../api/models';

export type TiltakstypeEntity = Entity<DatabaseDictionary, 'tiltakstype'>;

export function toTiltakstype(entity: TiltakstypeEntity): Tiltakstype {
  return {
    _id: entity.id,
    innsatsgruppe: {
      beskrivelse: entity.innsatsgruppe!!,
      _id: '',
      tittel: '',
    },
    tiltakstypeNavn: entity.tiltakstypeNavn,
  };
}
