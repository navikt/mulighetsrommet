import { CheckboxGroup } from "@mr/frontend-common";
import { Tiltakskode, TiltakstypeDto } from "@tiltaksadministrasjon/api-client";
import { useMemo } from "react";
import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";

interface Props {
  value: string[];
  onChange: (tiltakstyper: Tiltakskode[]) => void;
}

export function TiltakskodeFilter({ value, onChange }: Props) {
  const tiltakstyper = useTiltakstyper();
  const groups = useTiltakskodeFilter(tiltakstyper);
  return (
    <CheckboxGroup
      legend="Tiltakstyper"
      hideLegend
      value={value}
      onChange={(tiltakskoder) => onChange(tiltakskoder as Tiltakskode[])}
      items={groups}
    />
  );
}

function useTiltakskodeFilter(tiltakstyper: TiltakstypeDto[]) {
  return useMemo(() => {
    const tiltakstyperByGroup = Object.entries(
      Object.groupBy(tiltakstyper, (tiltakstype) => tiltakstype.gruppe || ""),
    );

    return tiltakstyperByGroup
      .flatMap(([gruppe, entries = []]) => {
        if (gruppe === "") {
          return entries.map((entry) => ({ id: entry.tiltakskode as string, navn: entry.navn }));
        } else {
          return {
            id: gruppe,
            navn: gruppe,
            items: entries.map((entry) => ({
              id: entry.tiltakskode,
              navn: entry.navn,
              erStandardvalg: true,
            })),
          };
        }
      })
      .toSorted((a, b) => a.navn.localeCompare(b.navn));
  }, [tiltakstyper]);
}
