import { Select } from "@navikt/ds-react";
import { useNavigate, useParams } from "react-router";
import { ArrangorflateArrangor } from "api-client";
import { pathByOrgnr } from "~/utils/navigation";

interface Props {
  arrangorer: ArrangorflateArrangor[];
}

export function Arrangorvelger({ arrangorer }: Props) {
  const navigate = useNavigate();
  const { orgnr } = useParams();

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
      {arrangorer.sort(compareByName).map((arrangor) => (
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

function compareByName(a: ArrangorflateArrangor, b: ArrangorflateArrangor) {
  return a.navn.localeCompare(b.navn);
}
