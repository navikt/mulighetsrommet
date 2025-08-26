import { Utdanningslop } from "@mr/api-client-v2";
import { Bolk } from "../detaljside/Bolk";
import { Metadata, Separator } from "../detaljside/Metadata";
import { List } from "@navikt/ds-react";
import { avtaletekster } from "../ledetekster/avtaleLedetekster";

interface Props {
  utdanningslop: Utdanningslop;
}

export function UtdanningslopDetaljer({ utdanningslop }: Props) {
  return (
    <>
      <Bolk>
        <Metadata
          header={avtaletekster.utdanning.utdanningsprogram.label}
          value={utdanningslop.utdanningsprogram.navn}
        />
      </Bolk>
      <Bolk>
        <Metadata
          header={avtaletekster.utdanning.laerefag.label}
          value={
            <List>
              {utdanningslop.utdanninger
                .sort((a, b) => a.navn.localeCompare(b.navn))
                .map((utdanning) => (
                  <List.Item key={utdanning.id}>{utdanning.navn}</List.Item>
                ))}
            </List>
          }
        />
      </Bolk>
      <Separator />
    </>
  );
}
