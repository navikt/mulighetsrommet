import { EarthIcon, EyeSlashIcon, ShieldLockIcon } from "@navikt/aksel-icons";
import { AkselColor } from "@navikt/ds-react/types/theme";
import { Gradering } from "@tiltaksadministrasjon/api-client";

export interface GraderingVisning {
  Ikon: typeof ShieldLockIcon;
  label: string;
  color: AkselColor;
}

export function graderingVisning(gradering: Gradering): GraderingVisning | null {
  switch (gradering) {
    case Gradering.STRENGT_FORTROLIG_ADRESSE:
      return { Ikon: ShieldLockIcon, label: "Strengt fortrolig adresse", color: "warning" };
    case Gradering.STRENGT_FORTROLIG_UTLAND:
      return { Ikon: ShieldLockIcon, label: "Strengt fortrolig utland", color: "warning" };
    case Gradering.FORTROLIG_ADRESSE:
      return { Ikon: ShieldLockIcon, label: "Fortrolig adresse", color: "warning" };
    case Gradering.SKJERMING:
      return { Ikon: EyeSlashIcon, label: "Skjermet", color: "info" };
    case Gradering.GEOGRAFISK:
      return {
        Ikon: EarthIcon,
        label: "Du har ikke tilgang til brukerens geografiske område",
        color: "info",
      };
    case Gradering.UGRADERT:
      return null;
  }
}
