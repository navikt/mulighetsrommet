import { Select } from "@navikt/ds-react";
import { useNavigate, useParams } from "react-router";
import { Arrangor } from "api-client";
import { pathByOrgnr } from "~/pathByOrgnr";

interface Props {
  arrangorer: Arrangor[];
}

export function Arrangorvelger({ arrangorer }: Props) {
  const navigate = useNavigate();
  const { orgnr } = useParams();

  const alfabetisk = (a: Arrangor, b: Arrangor) => a.navn.localeCompare(b.navn);

  return (
    <Select
      value={orgnr}
      label="Velg arrangør du vil representere"
      hideLabel
      name="orgnr"
      onChange={(e) => {
        navigate(pathByOrgnr(e.target.value).utbetalinger);
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
