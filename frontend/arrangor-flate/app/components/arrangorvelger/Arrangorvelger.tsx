import { Select } from "@navikt/ds-react";
import { useNavigate } from "@remix-run/react";
import { useOrgnrFromUrl } from "../../utils";

interface Props {
  arrangorer: { navn: string; organisasjonsnummer: string }[]; // TODO Bytt til modell fra OpenAPI
}

export function Arrangorvelger({ arrangorer }: Props) {
  const navigate = useNavigate();
  const currentOrgnr = useOrgnrFromUrl();

  return (
    <Select
      defaultValue={currentOrgnr}
      label="Velg arrangør du vil representere"
      hideLabel
      name="orgnr"
      onChange={(e) => {
        navigate(`${e.target.value}/refusjonskrav`);
      }}
    >
      {arrangorer.map((arrangor) => (
        <option key={arrangor.organisasjonsnummer} value={arrangor.organisasjonsnummer}>
          {arrangor.navn} - {arrangor.organisasjonsnummer}
        </option>
      ))}
    </Select>
  );
}
