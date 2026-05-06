import { EyeSlashIcon, ShieldLockIcon } from "@navikt/aksel-icons";
import { BodyShort, Tooltip } from "@navikt/ds-react";
import { Gradering } from "@tiltaksadministrasjon/api-client";

interface Props {
  navn: string;
  gradering: Gradering;
}

export function NavnOgGradering({ navn, gradering }: Props) {
  function graderingIkon() {
    switch (gradering) {
      case Gradering.STRENGT_FORTROLIG_ADRESSE:
        return (
          <Tooltip content="Strengt fortrolig adresse">
            <ShieldLockIcon
              style={{ color: "var(--ax-text-warning-decoration)" }}
              title="a11y-title"
              fontSize="5rem"
            />
          </Tooltip>
        );
      case Gradering.STRENGT_FORTROLIG_UTLAND:
        return (
          <Tooltip content="Strengt fortrolig utland">
            <ShieldLockIcon
              style={{ color: "var(--ax-text-warning-decoration)" }}
              title="a11y-title"
              fontSize="5rem"
            />
          </Tooltip>
        );
      case Gradering.FORTROLIG_ADRESSE:
        return (
          <Tooltip content="Fortrolig utland">
            <ShieldLockIcon
              style={{ color: "var(--ax-text-warning-decoration)" }}
              title="a11y-title"
              fontSize="5rem"
            />
          </Tooltip>
        );
      case Gradering.SKJERMING:
        return (
          <Tooltip content="Skjermet">
            <EyeSlashIcon
              style={{ color: "var(--ax-text-info-decoration)" }}
              title="a11y-title"
              fontSize="1.25rem"
            />
          </Tooltip>
        );
      case Gradering.UGRADERT:
        return null;
    }
  }

  return (
    <div style={{ display: "flex", alignItems: "center", gap: "0.25rem" }}>
      <BodyShort as="span" className="font-bold">
        {navn}
      </BodyShort>
      {graderingIkon()}
    </div>
  );
}
