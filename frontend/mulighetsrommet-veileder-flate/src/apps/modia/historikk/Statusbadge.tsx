import { DeltakerStatus, DeltakerStatusType } from "@mr/api-client";
import { Tag } from "@navikt/ds-react";
import classNames from "classnames";
import styles from "./Statusbadge.module.scss";

type Variant =
  | "warning"
  | "warning-filled"
  | "warning-moderate"
  | "error"
  | "error-filled"
  | "error-moderate"
  | "info"
  | "info-filled"
  | "info-moderate"
  | "success"
  | "success-filled"
  | "success-moderate"
  | "neutral"
  | "neutral-filled"
  | "neutral-moderate"
  | "alt1"
  | "alt1-filled"
  | "alt1-moderate"
  | "alt2"
  | "alt2-filled"
  | "alt2-moderate"
  | "alt3"
  | "alt3-filled"
  | "alt3-moderate"
  | "white"; // Ekstra variant for hvit bakgrunn

export function StatusBadge({ status }: { status: DeltakerStatus }) {
  const { variant, title } = variantAndTitle(status);

  return (
    <Tag
      size="small"
      variant={variant === "white" ? "neutral" : variant}
      className={classNames({ [styles.white]: variant === "white" })}
    >
      {title}
    </Tag>
  );
}

function variantAndTitle(status: DeltakerStatus): {
  variant: Variant;
  title: string;
} {
  switch (status.type) {
    case DeltakerStatusType.VENTER_PA_OPPSTART:
      return { variant: "alt3", title: "Venter på oppstart" };
    case DeltakerStatusType.DELTAR:
      return { variant: "white", title: "Deltar" };
    case DeltakerStatusType.HAR_SLUTTET:
      return { variant: "neutral", title: "Har sluttet" };
    case DeltakerStatusType.IKKE_AKTUELL:
      return { variant: "neutral", title: "Ikke aktuell" };
    case DeltakerStatusType.FEILREGISTRERT:
      return { variant: "neutral", title: "Feilregistrert" };
    case DeltakerStatusType.PABEGYNT_REGISTRERING:
      return { variant: "info", title: "Påbegynt registrering" };
    case DeltakerStatusType.SOKT_INN:
      return { variant: "alt3", title: "Søkt om plass" };
    case DeltakerStatusType.VURDERES:
      return { variant: "alt2", title: "Vurderes" };
    case DeltakerStatusType.VENTELISTE:
      return { variant: "neutral", title: "På venteliste" };
    case DeltakerStatusType.AVBRUTT:
      return { variant: "neutral", title: "Avbrutt" };
    case DeltakerStatusType.FULLFORT:
      return { variant: "alt1", title: "Fullført" };
    case DeltakerStatusType.UTKAST_TIL_PAMELDING:
      return { variant: "info", title: "Utkast til påmelding" };
    case DeltakerStatusType.AVBRUTT_UTKAST:
      return { variant: "neutral", title: "Avbrutt utkast" };
    case DeltakerStatusType.AVSLAG:
      return { variant: "neutral", title: "Fått avslag" };
    case DeltakerStatusType.TAKKET_NEI_TIL_TILBUD:
      return { variant: "neutral", title: "Takket nei til tilbud" };
    case DeltakerStatusType.TILBUD:
      return { variant: "info", title: "Godkjent tiltaksplass" };
    case DeltakerStatusType.TAKKET_JA_TIL_TILBUD:
      return { variant: "info", title: "Takket ja til tilbud" };
    case DeltakerStatusType.INFORMASJONSMOTE:
      return { variant: "white", title: "Informasjonsmøte" };
    case DeltakerStatusType.AKTUELL:
      return { variant: "info", title: "Aktuell" };
    case DeltakerStatusType.GJENNOMFORES:
      return { variant: "white", title: "Gjennomføres" };
    case DeltakerStatusType.DELTAKELSE_AVBRUTT:
      return { variant: "neutral", title: "Deltakelse avbrutt" };
    case DeltakerStatusType.GJENNOMFORING_AVBRUTT:
      return { variant: "neutral", title: "Gjennomføring avbrutt" };
    case DeltakerStatusType.GJENNOMFORING_AVLYST:
      return { variant: "neutral", title: "Gjenomføring avlyst" };
    case DeltakerStatusType.IKKE_MOTT:
      return { variant: "neutral", title: "Ikke møtt" };
    default:
      throw new Error(`Ukjent status: ${status}`);
  }
}
