import { AvtaleStatusDto, GjennomforingStatusDto, TiltakstypeStatus } from "@mr/api-client-v2";

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

export function getAvtaleStatusTagProps(status: AvtaleStatusDto["type"]): {
  variant: "success" | "neutral" | "error";
  name: string;
} {
  switch (status) {
    case "UTKAST":
      return { variant: "neutral", name: "Utkast" };
    case "AKTIV":
      return { variant: "success", name: "Aktiv" };
    case "AVSLUTTET":
      return { variant: "neutral", name: "Avsluttet" };
    case "AVBRUTT":
      return { variant: "error", name: "Avbrutt" };
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
