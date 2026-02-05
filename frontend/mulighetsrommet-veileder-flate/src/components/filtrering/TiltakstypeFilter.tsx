import { useTiltakstyper } from "@/api/queries/useTiltakstyper";
import { VeilederflateTiltakstype } from "@api-client";
import { CheckboxGroup } from "@mr/frontend-common";
import { useMemo } from "react";

interface TiltakstypeOption {
  id: string;
  tittel: string;
}

interface Props {
  value: TiltakstypeOption[];
  onChange: (value: TiltakstypeOption[]) => void;
}

export function TiltakstypeFilter(props: Props) {
  const { data: tiltakstyper } = useTiltakstyper();
  const groups = useTiltakstyperFilter(tiltakstyper);

  const value = props.value.map((option) => option.id);

  function onChange(value: string[]) {
    const selected = tiltakstyper
      .filter((tiltakstype) => value.includes(tiltakstype.sanityId))
      .map((tiltakstype) => ({
        id: tiltakstype.sanityId,
        tittel: tiltakstype.navn,
      }));
    props.onChange(selected);
  }

  return <CheckboxGroup value={value} onChange={onChange} groups={groups} />;
}

function useTiltakstyperFilter(tiltakstyper: VeilederflateTiltakstype[]) {
  return useMemo(() => {
    const tiltakstyperByGroup = Object.entries(
      Object.groupBy(tiltakstyper, (tiltakstype) => tiltakstype.tiltaksgruppe || ""),
    );

    return tiltakstyperByGroup
      .flatMap(([gruppe, entries = []]) => {
        if (gruppe === "") {
          return entries.map((entry) => ({ id: entry.sanityId, navn: entry.navn, items: [] }));
        } else {
          return {
            id: gruppe,
            navn: gruppe,
            items: entries.map((entry) => ({
              id: entry.sanityId,
              navn: entry.navn,
              erStandardvalg: true,
            })),
          };
        }
      })
      .toSorted((a, b) => a.navn.localeCompare(b.navn));
  }, [tiltakstyper]);
}
