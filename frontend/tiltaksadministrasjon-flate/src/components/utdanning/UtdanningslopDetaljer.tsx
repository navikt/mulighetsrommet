import { Definisjonsliste } from "@mr/frontend-common/components/definisjonsliste/Definisjonsliste";
import { avtaletekster } from "../ledetekster/avtaleLedetekster";
import { type UtdanningslopDetaljer } from "@tiltaksadministrasjon/api-client";

interface Props {
  utdanningslop: UtdanningslopDetaljer;
}

export function UtdanningslopDetaljer({ utdanningslop }: Props) {
  return (
    <Definisjonsliste
      title="Utdanningsløp"
      definitions={[
        {
          key: avtaletekster.utdanning.utdanningsprogram.label,
          value: utdanningslop.utdanningsprogram.navn,
        },
        {
          key: avtaletekster.utdanning.laerefag.label,
          value: (
            <ul>
              {utdanningslop.utdanninger
                .sort((a, b) => a.navn.localeCompare(b.navn))
                .map((utdanning) => (
                  <li className="list-disc list-inside" key={utdanning.id}>
                    {utdanning.navn}
                  </li>
                ))}
            </ul>
          ),
        },
      ]}
    />
  );
}
