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
