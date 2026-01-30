import { CheckboxGroup } from "@mr/frontend-common";
import { TiltakstypeDto } from "@tiltaksadministrasjon/api-client";
import { useMemo } from "react";

interface Props {
  tiltakstyper: TiltakstypeDto[];
  value: string[];
  onChange: (tiltakstyper: string[]) => void;
}

export function TiltakstypeFilter({ tiltakstyper, value, onChange }: Props) {
  const groups = useTiltakstyperFilter(tiltakstyper);
  return <CheckboxGroup value={value} onChange={onChange} groups={groups} />;
}

function useTiltakstyperFilter(tiltakstyper: TiltakstypeDto[]) {
  return useMemo(() => {
    const tiltakstyperByGroup = Object.entries(
      Object.groupBy(tiltakstyper, (tiltakstype) => tiltakstype.gruppe || ""),
    );

    return tiltakstyperByGroup
      .flatMap(([gruppe, entries = []]) => {
        if (gruppe === "") {
          return entries.map((entry) => ({ id: entry.id, navn: entry.navn, items: [] }));
        } else {
          return {
            id: gruppe,
            navn: gruppe,
            items: entries.map((entry) => ({
              id: entry.id,
              navn: entry.navn,
              erStandardvalg: true,
            })),
          };
        }
      })
      .toSorted((a, b) => a.navn.localeCompare(b.navn));
  }, [tiltakstyper]);
}
