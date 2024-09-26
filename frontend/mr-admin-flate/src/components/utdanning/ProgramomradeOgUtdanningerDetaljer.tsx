import { ProgramomradeMedUtdanninger } from "@mr/api-client";
import { Bolk } from "../detaljside/Bolk";
import { Metadata, Separator } from "../detaljside/Metadata";
import { List } from "@navikt/ds-react";
import { avtaletekster } from "../ledetekster/avtaleLedetekster";

interface Props {
  programomradeMedUtdanninger: ProgramomradeMedUtdanninger;
}

export function ProgramomradeOgUtdanningerDetaljer({ programomradeMedUtdanninger }: Props) {
  return (
    <>
      <Bolk>
        <Metadata
          header={avtaletekster.programomradeOgUtdanninger.programomradeLabel}
          verdi={programomradeMedUtdanninger.programomrade.navn}
        />
      </Bolk>
      <Bolk>
        <Metadata
          header={avtaletekster.programomradeOgUtdanninger.sluttkompetanserLabel}
          verdi={
            <List>
              {programomradeMedUtdanninger.utdanninger
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
