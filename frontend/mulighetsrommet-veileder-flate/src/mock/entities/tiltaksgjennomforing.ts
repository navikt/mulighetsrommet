import { Entity } from '@mswjs/data/lib/glossary';
import { Tiltaksgjennomforing, Tiltakstype } from 'mulighetsrommet-api-client';
import { DatabaseDictionary } from '../database';

export type TiltaksgjennomforingEntity = Entity<DatabaseDictionary, 'tiltaksgjennomforing'>;

export function toTiltaksgjennomforing(entity: TiltaksgjennomforingEntity): Tiltaksgjennomforing {
  return {
    _id: entity.id,
    tiltaksnummer: entity.tiltaksnummer,
    tiltaksgjennomforingNavn: entity.tiltaksgjennomforingNavn,
    beskrivelse: entity.beskrivelse,
    kontaktinfoArrangor: {
      _id: entity.kontaktinfoArrangor.id!,
      selskapsnavn: entity.kontaktinfoArrangor.selskapsnavn!,
      adresse: entity.kontaktinfoArrangor.adresse!,
      epost: entity.kontaktinfoArrangor.epost!,
      telefonnummer: entity.kontaktinfoArrangor.telefonnummer!,
    },
    tiltakstype: entity.tiltakstype as Tiltakstype,
    lokasjon: entity.lokasjon,
    enheter: {
      fylke: entity.enheter.fylke!,
    },
    oppstart: entity.oppstart,
    kontaktinfoTiltaksansvarlig: {
      _id: entity.kontaktinfoTiltaksansvarlig.id!,
      enhet: entity.kontaktinfoTiltaksansvarlig.enhet!,
      epost: entity.kontaktinfoTiltaksansvarlig.epost!,
      telefonnummer: entity.kontaktinfoTiltaksansvarlig.telefonnummer!,
      navn: entity.kontaktinfoTiltaksansvarlig.navn!,
    },
  };
}
