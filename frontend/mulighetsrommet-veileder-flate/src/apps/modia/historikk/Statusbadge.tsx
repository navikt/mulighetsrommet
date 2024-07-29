import { AmtDeltakerStatus, ArenaDeltakerStatus } from "mulighetsrommet-api-client";
import { Tag } from "@navikt/ds-react";
import styles from "./Statusbadge.module.scss";
import classNames from "classnames";

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

export function StatusBadge({ status }: { status: AmtDeltakerStatus | ArenaDeltakerStatus }) {
  const { variant, title } = variantAndTitle(status);

  return (
    <Tag
      size="small"
      variant={variant === "white" ? "neutral" : variant}
      className={classNames(variant === "white" && styles.white)}
    >
      {title}
    </Tag>
  );
}

function isAmtDeltakerStatus(
  status: AmtDeltakerStatus | ArenaDeltakerStatus,
): status is AmtDeltakerStatus {
  return typeof status === "object";
}

function variantAndTitle(status: AmtDeltakerStatus | ArenaDeltakerStatus): {
  variant: Variant;
  title: string;
} {
  if (isAmtDeltakerStatus(status)) {
    switch (status.type) {
      case "VENTER_PA_OPPSTART":
        return { variant: "alt3", title: "Venter på oppstart" };
      case "DELTAR":
        return { variant: "white", title: "Deltar" };
      case "HAR_SLUTTET":
        return { variant: "neutral", title: "Har sluttet" };
      case "IKKE_AKTUELL":
        return { variant: "neutral", title: "Ikke aktuell" };
      case "FEILREGISTRERT":
        return { variant: "neutral", title: "Feilregistrert" };
      case "PABEGYNT_REGISTRERING":
        return { variant: "info", title: "Påbegynt registrering" };
      case "SOKT_INN":
        return { variant: "alt3", title: "Søkt om plass" };
      case "VURDERES":
        return { variant: "alt2", title: "Vurderes" };
      case "VENTELISTE":
        return { variant: "neutral", title: "På venteliste" };
      case "AVBRUTT":
        return { variant: "neutral", title: "Avbrutt" };
      case "FULLFORT":
        return { variant: "alt1", title: "Fullført" };
      case "UTKAST_TIL_PAMELDING":
        return { variant: "info", title: "Utkast til påmelding" };
      case "AVBRUTT_UTKAST":
        return { variant: "neutral", title: "Avbrutt utkast" };
    }
  } else {
    switch (status) {
      case ArenaDeltakerStatus.AVSLAG:
        return { variant: "neutral", title: "Fått avslag" };
      case ArenaDeltakerStatus.IKKE_AKTUELL:
        return { variant: "neutral", title: "Ikke aktuell" };
      case ArenaDeltakerStatus.TAKKET_NEI_TIL_TILBUD:
        return { variant: "neutral", title: "Takket nei til tilbud" };
      case ArenaDeltakerStatus.TILBUD:
        return { variant: "info", title: "Tilbud" };
      case ArenaDeltakerStatus.TAKKET_JA_TIL_TILBUD:
        return { variant: "info", title: "Takket ja til tilbud" };
      case ArenaDeltakerStatus.INFORMASJONSMOTE:
        return { variant: "white", title: "Informasjonsmøte" };
      case ArenaDeltakerStatus.AKTUELL:
        return { variant: "info", title: "Aktuell" };
      case ArenaDeltakerStatus.VENTELISTE:
        return { variant: "neutral", title: "Venteliste" };
      case ArenaDeltakerStatus.GJENNOMFORES:
        return { variant: "white", title: "Gjennomføres" };
      case ArenaDeltakerStatus.DELTAKELSE_AVBRUTT:
        return { variant: "neutral", title: "Deltakelse avbrutt" };
      case ArenaDeltakerStatus.GJENNOMFORING_AVBRUTT:
        return { variant: "neutral", title: "Gjennomføring avbrutt" };
      case ArenaDeltakerStatus.GJENNOMFORING_AVLYST:
        return { variant: "neutral", title: "Gjenomføring avlyst" };
      case ArenaDeltakerStatus.FULLFORT:
        return { variant: "alt1", title: "Fullført" };
      case ArenaDeltakerStatus.IKKE_MOTT:
        return { variant: "neutral", title: "Ikke møtt" };
    }
  }
}
