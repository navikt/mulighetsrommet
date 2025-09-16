import { GjennomforingStatusDto } from "@mr/api-client-v2";
import { TiltakstypeStatus } from "@tiltaksadministrasjon/api-client";

// TODO: flytt mappinglogikk til BE
export function getTiltakstypeStatusTagProps(status: TiltakstypeStatus): {
  variant: "success" | "neutral";
  name: string;
} {
  switch (status) {
    case TiltakstypeStatus.AKTIV:
      return { variant: "success", name: "Aktiv" };
    case TiltakstypeStatus.AVSLUTTET:
      return { variant: "neutral", name: "Avsluttet" };
  }
}

export function getGjennomforingStatusTagsProps(status: GjennomforingStatusDto["type"]): {
  variant: "alt1" | "success" | "neutral" | "error";
  name: string;
} {
  switch (status) {
    case "GJENNOMFORES":
      return { variant: "success", name: "Gjennomføres" };
    case "AVSLUTTET":
      return { variant: "neutral", name: "Avsluttet" };
    case "AVBRUTT":
      return { variant: "error", name: "Avbrutt" };
    case "AVLYST":
      return { variant: "error", name: "Avlyst" };
  }
}
