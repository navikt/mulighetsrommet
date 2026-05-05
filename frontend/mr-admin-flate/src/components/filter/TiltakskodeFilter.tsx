import { CheckboxGroup } from "@mr/frontend-common";
import { Tiltakskode, TiltakstypeKompaktDto } from "@tiltaksadministrasjon/api-client";
import { useMemo } from "react";

interface Props {
  tiltakstyper: TiltakstypeKompaktDto[];
  value: string[];
  onChange: (tiltakskoder: Tiltakskode[]) => void;
}

export function TiltakskodeFilter({ tiltakstyper, value, onChange }: Props) {
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

function useTiltakskodeFilter(tiltakstyper: TiltakstypeKompaktDto[]) {
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
