import { useForhandsgodkjenteSatser } from "@/api/tilsagn/useForhandsgodkjenteSatser";
import { HGrid, HStack, VStack } from "@navikt/ds-react";
import { avtaleLoader } from "@/pages/avtaler/avtaleLoader";
import { useLoaderData } from "react-router";
import { Bolk } from "@/components/detaljside/Bolk";
import { Metadata } from "@/components/detaljside/Metadata";
import { AvtaleDto, Prismodell } from "@mr/api-client";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { BorderedContainer } from "@/components/skjema/BorderedContainer";
import { formaterDato } from "@/utils/Utils";
import { formaterTall } from "@mr/frontend-common/utils/utils";
import { DetaljerInfoContainer } from "@/pages/DetaljerInfoContainer";

export function AvtalePrisOgFaktureringDetaljer() {
  const { avtale } = useLoaderData<typeof avtaleLoader>();

  const prismodell = avtale.prismodell;

  return (
    <HGrid columns={2} align="start">
      <DetaljerInfoContainer>
        <Bolk>
          <Metadata header={avtaletekster.tiltakstypeLabel} verdi={avtale.tiltakstype.navn} />
        </Bolk>

        <Bolk>
          <Metadata
            header={avtaletekster.prismodell.label}
            verdi={prismodell ? avtaletekster.prismodell.beskrivelse(prismodell) : null}
          />
        </Bolk>

        {prismodell === Prismodell.FORHANDSGODKJENT && (
          <ForhandsgodkjentAvtalePrismodell avtale={avtale} />
        )}
      </DetaljerInfoContainer>
    </HGrid>
  );
}

interface ForhandsgodkjentAvtalePrismodellProps {
  avtale: AvtaleDto;
}

function ForhandsgodkjentAvtalePrismodell({ avtale }: ForhandsgodkjentAvtalePrismodellProps) {
  const { data: satser } = useForhandsgodkjenteSatser(avtale.tiltakstype.tiltakskode);

  if (!satser) return null;

  return (
    <VStack gap="4">
      {satser.map((sats) => (
        <BorderedContainer key={sats.periodeStart}>
          <HStack gap="4">
            <Metadata header={avtaletekster.prismodell.valuta.label} verdi={sats.valuta} />

            <Metadata
              header={avtaletekster.prismodell.pris.label}
              verdi={formaterTall(sats.pris)}
            />

            <Metadata
              header={avtaletekster.prismodell.periodeStart.label}
              verdi={formaterDato(sats.periodeStart)}
            />

            <Metadata
              header={avtaletekster.prismodell.periodeSlutt.label}
              verdi={formaterDato(sats.periodeSlutt)}
            />
          </HStack>
        </BorderedContainer>
      ))}
    </VStack>
  );
}
