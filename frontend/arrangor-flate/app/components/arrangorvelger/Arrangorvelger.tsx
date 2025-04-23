import { Select } from "@navikt/ds-react";
import { useNavigate, useParams } from "react-router";
import { Arrangor } from "api-client";

interface Props {
  arrangorer: Arrangor[];
}

export function Arrangorvelger({ arrangorer }: Props) {
  const navigate = useNavigate();
  const { currentOrgnr } = useParams();

  const alfabetisk = (a: Arrangor, b: Arrangor) => a.navn.localeCompare(b.navn);

  return (
    <Select
      value={currentOrgnr}
      label="Velg arrangør du vil representere"
      hideLabel
      name="orgnr"
      onChange={(e) => {
        navigate(`${e.target.value}/utbetaling`);
      }}
      className="w-80"
    >
      {arrangorer.sort(alfabetisk).map((arrangor) => (
        <option
          key={arrangor.organisasjonsnummer}
          value={arrangor.organisasjonsnummer}
          title={`${arrangor.navn} - ${arrangor.organisasjonsnummer}`}
        >
          {arrangor.navn} - {arrangor.organisasjonsnummer}
        </option>
      ))}
    </Select>
  );
}
