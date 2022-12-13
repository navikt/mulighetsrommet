import { Select } from "@navikt/ds-react";
import React from "react";
import { useVisForMiljo } from "../../hooks/useVisForMiljo";

const ROLLE_KEY_LS = "valp-rolle-adminflate";

interface Props {
  gjelderForMiljo: string[];
}

export function EndreRolleVedLokalUtvikling({ gjelderForMiljo }: Props) {
  const visForMiljo = useVisForMiljo(gjelderForMiljo);
  if (!visForMiljo) return null;

  const rolleValgtFraLocalStorage =
    window.localStorage.getItem(ROLLE_KEY_LS) ?? "tiltaksansvarlig";

  const onRolleChanged = (e: React.ChangeEvent<HTMLSelectElement>) => {
    e.preventDefault();
    window.localStorage.setItem(ROLLE_KEY_LS, e.currentTarget.value);
    window.location.reload();
  };

  return (
    <details style={{ padding: "1rem" }}>
      <summary>Velg rolle</summary>
      <Select
        onChange={onRolleChanged}
        label="Velg rolle"
        hideLabel
        size="small"
        style={{ width: "10rem" }}
        value={rolleValgtFraLocalStorage}
      >
        <option value="tiltaksansvarlig">Tiltaksansvarlig</option>
        <option value="fagansvarlig">Fagansvarlig</option>
      </Select>
    </details>
  );
}
