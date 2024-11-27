import { TiltaksgjennomforingDto } from "@mr/api-client";
import { GjennomforingStatusTag } from "@mr/frontend-common";
import { Box, BoxProps, HGrid } from "@navikt/ds-react";
import { formaterDato } from "../../utils/Utils";
import { Metadata } from "../detaljside/Metadata";

interface Props {
  tiltaksgjennomforing: TiltaksgjennomforingDto;
  borderWidth?: BoxProps["borderWidth"];
}

export function TiltakDetaljerForTilsagn({ tiltaksgjennomforing, borderWidth = "1" }: Props) {
  return (
    <>
      <Box
        borderColor="border-subtle"
        paddingBlock={"4 10"}
        borderWidth={borderWidth}
        borderRadius="medium"
      >
        <HGrid columns="2fr 2fr 2fr 1fr 1fr 1fr 1fr">
          <Metadata header="Tiltaksnavn" verdi={tiltaksgjennomforing.navn} />
          <Metadata
            header="Arrangør"
            verdi={`${tiltaksgjennomforing.arrangor.navn} - ${tiltaksgjennomforing.arrangor.organisasjonsnummer}`}
          />
          <Metadata header="Tiltaksnummer" verdi={tiltaksgjennomforing.tiltaksnummer} />
          <Metadata header="Startdato" verdi={formaterDato(tiltaksgjennomforing.startDato)} />
          <Metadata header="Sluttdato" verdi={formaterDato(tiltaksgjennomforing.startDato)} />
          <Metadata header="Antall plasser" verdi={tiltaksgjennomforing.antallPlasser} />
          <Metadata
            header="Status"
            verdi={<GjennomforingStatusTag status={tiltaksgjennomforing.status.status} />}
          />
        </HGrid>
      </Box>
    </>
  );
}
