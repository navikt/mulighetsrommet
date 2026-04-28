import { useTiltakstyper } from "@/api/queries/useTiltakstyper";
import { Tiltakskode, VeilederflateTiltakstype } from "@api-client";
import { CheckboxGroup } from "@mr/frontend-common";
import { useMemo } from "react";

interface TiltakstypeOption {
  id: Tiltakskode;
  tittel: string;
}

interface Props {
  value: TiltakstypeOption[];
  onChange: (value: TiltakstypeOption[]) => void;
}

export function TiltakstypeFilter(props: Props) {
  const { data: tiltakstyper } = useTiltakstyper();
  const groups = useTiltakstyperFilter(tiltakstyper);

  const value: string[] = props.value.map((option) => option.id);

  function onChange(value: string[]) {
    const selected = tiltakstyper
      .filter((tiltakstype) => value.includes(tiltakstype.tiltakskode))
      .map((tiltakstype) => ({
        id: tiltakstype.tiltakskode,
        tittel: tiltakstype.navn,
      }));
    props.onChange(selected);
  }

  return (
    <CheckboxGroup
      legend="Tiltakstyper"
      hideLegend
      value={value}
      onChange={onChange}
      items={groups}
    />
  );
}

function useTiltakstyperFilter(tiltakstyper: VeilederflateTiltakstype[]) {
  return useMemo(() => {
    const tiltakstyperByGroup = Object.entries(
      Object.groupBy(tiltakstyper, (tiltakstype) => tiltakstype.tiltaksgruppe || ""),
    );

    return tiltakstyperByGroup
      .flatMap(([gruppe, entries = []]) => {
        if (gruppe === "") {
          return entries.map((entry) => ({ id: entry.tiltakskode as string, navn: entry.navn }));
        } else {
          return {
            id: gruppe,
            navn: gruppe,
            items: entries.toSorted(compareByNavn).map((entry) => ({
              id: entry.tiltakskode as string,
              navn: entry.navn,
              erStandardvalg: true,
            })),
          };
        }
      })
      .toSorted(compareByNavn);
  }, [tiltakstyper]);
}

function compareByNavn(a: { navn: string }, b: { navn: string }) {
  return a.navn.localeCompare(b.navn);
}
