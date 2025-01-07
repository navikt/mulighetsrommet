import { Select } from "@navikt/ds-react";
import { useNavigate } from "react-router";
import { useOrgnrFromUrl } from "../../utils";
import { Arrangor } from "@mr/api-client-v2";

interface Props {
  arrangorer: Arrangor[];
}

export function Arrangorvelger({ arrangorer }: Props) {
  const navigate = useNavigate();
  const currentOrgnr = useOrgnrFromUrl();

  const alfabetisk = (a: Arrangor, b: Arrangor) => a.navn.localeCompare(b.navn);

  return (
    <Select
      value={currentOrgnr}
      label="Velg arrangør du vil representere"
      hideLabel
      name="orgnr"
      onChange={(e) => {
        navigate(`${e.target.value}/refusjonskrav`);
      }}
    >
      {arrangorer.sort(alfabetisk).map((arrangor) => (
        <option key={arrangor.organisasjonsnummer} value={arrangor.organisasjonsnummer}>
          {arrangor.navn} - {arrangor.organisasjonsnummer}
        </option>
      ))}
    </Select>
  );
}
