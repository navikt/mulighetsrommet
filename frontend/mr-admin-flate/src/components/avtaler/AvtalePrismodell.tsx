import { usePrismodeller } from "@/api/tilsagn/usePrismodeller";
import { AvtaleDto } from "@mr/api-client-v2";
import { Box, Heading, HStack, VStack } from "@navikt/ds-react";
import { Metadata } from "../detaljside/Metadata";
import { avtaletekster } from "../ledetekster/avtaleLedetekster";
import { formaterDato } from "@mr/frontend-common/utils/date";
import { formaterTall } from "@mr/frontend-common/utils/utils";
import { Definisjonsliste } from "@mr/frontend-common/components/definisjonsliste/Definisjonsliste";
import Prisbetingelser from "../utbetaling/Prisbetingelser";
import { useForhandsgodkjenteSatser } from "@/api/tilsagn/useForhandsgodkjenteSatser";

export function AvtalePrismodell({ avtale }: { avtale: AvtaleDto }) {
  const { data: prismodeller = [] } = usePrismodeller(avtale.tiltakstype.tiltakskode);

  const beskrivelse = prismodeller.find(({ type }) => type === avtale.prismodell.type)?.beskrivelse;

  switch (avtale.prismodell.type) {
    case "FORHANDSGODKJENT_PRIS_PER_MANEDSVERK":
      return (
        <Box>
          <Heading level="3" size="small" spacing>
            {avtaletekster.prismodell.heading}
          </Heading>
          <AvtalteSatser avtale={avtale} prismodellBeskrivelse={beskrivelse} />
        </Box>
      );
    case "AVTALT_PRIS_PER_MANEDSVERK":
    case "AVTALT_PRIS_PER_UKESVERK":
      return (
        <Box>
          <Heading level="3" size="small" spacing>
            {avtaletekster.prismodell.heading}
          </Heading>
          <VStack gap="4">
            <Heading level="4" size="xsmall">
              Prismodell: {beskrivelse}
            </Heading>
            {avtale.prismodell.satser.map((sats) => (
              <Box
                key={sats.periodeStart}
                borderColor="border-subtle"
                padding="2"
                borderWidth="1"
                borderRadius="medium"
              >
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
              </Box>
            ))}
            <Definisjonsliste
              columns={1}
              headingLevel="4"
              definitions={[
                {
                  key: avtaletekster.prisOgBetalingLabel,
                  value: avtale.prismodell?.prisbetingelser ? (
                    <Prisbetingelser value={avtale.prismodell.prisbetingelser} />
                  ) : (
                    "-"
                  ),
                },
              ]}
            />
          </VStack>
        </Box>
      );
    case "ANNEN_AVTALT_PRIS":
      return (
        <Box>
          <Heading level="3" size="small" spacing>
            {avtaletekster.prismodell.heading}
          </Heading>
          <VStack gap="4">
            <Heading level="4" size="xsmall">
              Prismodell: {beskrivelse}
            </Heading>
            <Definisjonsliste
              columns={1}
              headingLevel="4"
              definitions={[
                {
                  key: avtaletekster.prisOgBetalingLabel,
                  value: avtale.prismodell?.prisbetingelser ? (
                    <Prisbetingelser value={avtale.prismodell.prisbetingelser} />
                  ) : (
                    "-"
                  ),
                },
              ]}
            />
          </VStack>
        </Box>
      );
  }
}

function AvtalteSatser({
  avtale,
  prismodellBeskrivelse,
}: {
  avtale: AvtaleDto;
  prismodellBeskrivelse: string | undefined;
}) {
  const { data: satser = [] } = useForhandsgodkjenteSatser(avtale.tiltakstype.tiltakskode);
  return (
    <Box>
      <Heading level="4" size="xsmall" spacing>
        Prismodell: {prismodellBeskrivelse}
      </Heading>
      {satser.map((sats) => (
        <Box
          key={sats.periodeStart}
          borderColor="border-subtle"
          padding="2"
          borderWidth="1"
          borderRadius="medium"
        >
          <HStack gap="4" key={sats.periodeStart}>
            <Metadata header={avtaletekster.prismodell.valuta.label} verdi={sats.valuta} />
            <Metadata
              header={avtaletekster.prismodell.sats.label}
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
        </Box>
      ))}
    </Box>
  );
}
