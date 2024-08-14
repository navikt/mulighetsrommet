import { TiltaksgjennomforingStatus } from "@mr/api-client";
import { shallowEquals } from "./shallow-equals";

export function addOrRemove<T>(array: T[], item: T): T[] {
  const exists = array.some(a => shallowEquals(a, item));

  if (exists) {
    return array.filter((c) => {
      return !shallowEquals(c, item);
    });
  } else {
    const result = array;
    result.push(item);
    return result;
  }
}

export function gjennomforingIsAktiv(
  status: TiltaksgjennomforingStatus,
): boolean {
  switch (status) {
    case TiltaksgjennomforingStatus.PLANLAGT:
    case TiltaksgjennomforingStatus.GJENNOMFORES:
      return true;
    case TiltaksgjennomforingStatus.AVBRUTT:
    case TiltaksgjennomforingStatus.AVLYST:
    case TiltaksgjennomforingStatus.AVSLUTTET:
      return false;
  }
}

