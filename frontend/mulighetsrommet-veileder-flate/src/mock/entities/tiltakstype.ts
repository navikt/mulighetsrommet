import { Entity } from '@mswjs/data/lib/glossary';
import { Tiltakskode, Tiltakstype } from 'mulighetsrommet-api-client';
import { DatabaseDictionary } from '../database';

export type TiltakstypeEntity = Entity<DatabaseDictionary, 'tiltakstype'>;

export function toTiltakstype(entity: TiltakstypeEntity): Tiltakstype {
  return {
    id: entity.id,
    innsatsgruppe: entity.innsatsgruppe?.id ?? null,
    sanityId: entity.sanityId,
    navn: entity.navn,
    tiltakskode: Tiltakskode[entity.tiltakskode as keyof typeof Tiltakskode],
    fraDato: entity.fraDato,
    tilDato: entity.tilDato,
    createdBy: entity.createdBy,
    createdAt: entity.createdAt,
    updatedBy: entity.updatedBy,
    updatedAt: entity.updatedAt,
  };
}
