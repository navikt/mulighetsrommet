import { faro } from "@grafana/faro-web-sdk";
import { Lenkeknapp } from "../lenkeknapp/Lenkeknapp";
import React from "react";
import { Button } from "@navikt/ds-react";
import { WritableAtom, useAtom } from "jotai";
import { AvtaleFilterProps, defaultAvtaleFilter } from "../../api/atoms";

interface Props {
  filterAtom: WritableAtom<AvtaleFilterProps, [newValue: AvtaleFilterProps], void>;
}

export function AvtaleFilterButtons({ filterAtom }: Props) {
  const [filter, setFilter] = useAtom(filterAtom);

  return (
    <div
      style={{
        display: "flex",
        flexDirection: "row",
        justifyContent: "space-between",
        height: "100%",
        alignItems: "center",
      }}
    >
      {filter.sok.length > 0 ||
      filter.navRegioner.length > 0 ||
      filter.tiltakstyper.length > 0 ||
      filter.statuser.length > 0 ||
      filter.leverandor_orgnr.length > 0 ? (
        <Button
          type="button"
          size="small"
          style={{ maxWidth: "130px" }}
          variant="tertiary"
          onClick={() => {
            setFilter({ ...defaultAvtaleFilter });
          }}
        >
          Nullstill filter
        </Button>
      ) : (
        <div></div>
      )}
      <Lenkeknapp
        to={`/avtaler/skjema`}
        size="small"
        variant="primary"
        handleClick={() => {
          faro?.api?.pushEvent("Bruker trykket på 'Opprett ny avtale'-knapp");
        }}
      >
        Opprett ny avtale
      </Lenkeknapp>
    </div>
  );
}
