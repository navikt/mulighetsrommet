import { Entity } from '@mswjs/data/lib/glossary';
import { Tiltaksgjennomforing, Tiltakskode } from 'mulighetsrommet-api';
import { DatabaseDictionary } from '../database';

export type TiltaksgjennomforingEntity = Entity<DatabaseDictionary, 'tiltaksgjennomforing'>;

export function toTiltaksgjennomforing(entity: TiltaksgjennomforingEntity): Tiltaksgjennomforing {
  return {
    id: entity.id,
    tiltaksnummer: entity.tiltaksnummer,
    tiltakskode: Tiltakskode[entity.tiltakskode as keyof typeof Tiltakskode],
    tittel: entity.tittel,
    beskrivelse: entity.beskrivelse,
    fraDato: entity.fraDato,
    tilDato: entity.tilDato,
  };
}
