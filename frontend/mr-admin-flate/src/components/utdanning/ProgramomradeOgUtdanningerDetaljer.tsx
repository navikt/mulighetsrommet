import { ProgramomradeMedUtdanninger } from "@mr/api-client";
import { Bolk } from "../detaljside/Bolk";
import { Metadata, Separator } from "../detaljside/Metadata";
import { List } from "@navikt/ds-react";

interface Props {
  programomradeMedUtdanninger: ProgramomradeMedUtdanninger;
}

export function ProgramomradeOgUtdanningerDetaljer({ programomradeMedUtdanninger }: Props) {
  return (
    <>
      <Bolk>
        <Metadata header="Programområde" verdi={programomradeMedUtdanninger.programomrade.navn} />
      </Bolk>
      <Bolk>
        <Metadata
          header="Utdanninger"
          verdi={
            <List>
              {programomradeMedUtdanninger.utdanninger.map((utdanning) => (
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
