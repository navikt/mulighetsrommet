import { Entity } from '@mswjs/data/lib/glossary';
import { Tiltaksgjennomforing } from '../../api';
import { DatabaseDictionary } from '../database';

export type TiltaksgjennomføringEntity = Entity<DatabaseDictionary, 'tiltaksgjennomforing'>;

export function toTiltaksgjennomforing(entity: TiltaksgjennomføringEntity): Tiltaksgjennomforing {
  return {
    id: entity.id,
    tiltaksnummer: entity.tiltaksnummer,
    tiltaksvariantId: entity.tiltaksvariantId?.id ?? -1,
    tittel: entity.tittel,
    beskrivelse: entity.beskrivelse,
    fraDato: entity.fraDato,
    tilDato: entity.tilDato,
  };
}
