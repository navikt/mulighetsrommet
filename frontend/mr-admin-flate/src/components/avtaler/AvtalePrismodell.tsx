import { usePrismodeller } from "@/api/tilsagn/usePrismodeller";
import {
  AvtaleDto,
  Prismodell,
  PrismodellDto,
  PrismodellDtoAnnenAvtaltPris,
} from "@mr/api-client-v2";
import { Box, Heading, HStack, VStack } from "@navikt/ds-react";
import { Metadata } from "../detaljside/Metadata";
import { avtaletekster } from "../ledetekster/avtaleLedetekster";
import { formaterDato } from "@mr/frontend-common/utils/date";
import { formaterTall } from "@mr/frontend-common/utils/utils";
import { Definisjonsliste } from "@mr/frontend-common/components/definisjonsliste/Definisjonsliste";
import { useForhandsgodkjenteSatser } from "@/api/tilsagn/useForhandsgodkjenteSatser";
import Fritekstfelt from "../detaljside/Fritekstfelt";

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
            <Metadata header={avtaletekster.prismodell.label} verdi={beskrivelse} />
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
                  value: avtale.prismodell.prisbetingelser ? (
                    <Fritekstfelt text={avtale.prismodell.prisbetingelser} />
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
      return <AnnenAvtaltPrismodell avtale={avtale} prismodellBeskrivelse={beskrivelse} />;
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
      <VStack gap="4">
        <Metadata header={avtaletekster.prismodell.label} verdi={prismodellBeskrivelse} />
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
      </VStack>
    </Box>
  );
}

export function AnnenAvtaltPrismodell({
  avtale,
  prismodellBeskrivelse,
}: {
  avtale: AvtaleDto;
  prismodellBeskrivelse: string | undefined;
}) {
  if (!isAnnenAvtaltPrismodell(avtale.prismodell)) {
    return null;
  }
  return (
    <Box>
      <Heading level="3" size="small" spacing>
        {avtaletekster.prismodell.heading}
      </Heading>
      <VStack gap="4">
        <Metadata header={avtaletekster.prismodell.label} verdi={prismodellBeskrivelse} />
        <Definisjonsliste
          columns={1}
          headingLevel="4"
          definitions={[
            {
              key: avtaletekster.prisOgBetalingLabel,
              value: <Fritekstfelt text={avtale.prismodell.prisbetingelser ?? "-"} />,
            },
          ]}
        />
      </VStack>
    </Box>
  );
}

const isAnnenAvtaltPrismodell = (obj: PrismodellDto): obj is PrismodellDtoAnnenAvtaltPris =>
  obj.type === Prismodell.ANNEN_AVTALT_PRIS;
