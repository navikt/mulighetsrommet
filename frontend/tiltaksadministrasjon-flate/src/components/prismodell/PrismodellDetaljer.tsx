import { BodyShort, Box, Heading, HStack, List, VStack } from "@navikt/ds-react";
import {
  MetadataFritekstfelt,
  MetadataVStack,
  Separator,
} from "@mr/frontend-common/components/datadriven/Metadata";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { AvtaltSatsDto, PrismodellDto, PrismodellType } from "@tiltaksadministrasjon/api-client";
import { formaterDato } from "@mr/frontend-common/utils/date";
import { formaterValuta } from "@mr/frontend-common/utils/utils";
import { ingenKostnaderAarsakToString, opplaeringTilskuddToString } from "@/utils/Utils";

interface PrismodellDetaljerProps {
  prismodell: PrismodellDto;
}

export function PrismodellDetaljer({ prismodell }: PrismodellDetaljerProps) {
  switch (prismodell.type) {
    case PrismodellType.FAST_SATS_PER_BENYTTET_PLASS_PER_MANED:
    case PrismodellType.FAST_SATS_PER_AVTALT_PLASS_PER_MANED:
      return <FastSats prismodell={prismodell} />;
    case PrismodellType.AVTALT_PRIS_PER_BENYTTET_PLASS_PER_MANED:
    case PrismodellType.AVTALT_PRIS_PER_BENYTTET_PLASS_PER_UKE:
    case PrismodellType.AVTALT_PRIS_PER_BENYTTET_PLASS_PER_HELE_UKE:
    case PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER:
      return <AvtaltPris prismodell={prismodell} />;
    case PrismodellType.ANNEN_AVTALT_PRIS:
      return <AnnenAvtaltPris prismodell={prismodell} />;
    case PrismodellType.TILSKUDD_TIL_OPPLAERING:
      return <BetalingsbetingelserTilskudd prismodell={prismodell} />;
    case PrismodellType.INGEN_KOSTNADER:
      return <BetalingsbetingelserIngenKostnader prismodell={prismodell} />;
  }
}

function FastSats({ prismodell }: PrismodellDetaljerProps) {
  return (
    <VStack key={prismodell.navn} gap="space-16">
      <PrismodellTypenavn type={prismodell.navn} />
      <PrismodellSatser satser={prismodell.satser} />
    </VStack>
  );
}

function AvtaltPris({ prismodell }: PrismodellDetaljerProps) {
  return (
    <VStack key={prismodell.navn} gap="space-16">
      <PrismodellTypenavn type={prismodell.navn} />
      <PrismodellSatser satser={prismodell.satser} />
      {prismodell.prisbetingelser && (
        <PrismodellPrisbetingelser prisbetingelser={prismodell.prisbetingelser} />
      )}
    </VStack>
  );
}

function AnnenAvtaltPris({ prismodell }: PrismodellDetaljerProps) {
  return (
    <VStack key={prismodell.navn} gap="space-16">
      <PrismodellTypenavn type={prismodell.navn} />
      <MetadataVStack
        label={avtaletekster.prismodell.tilsagnPerDeltaker.label}
        value={prismodell.tilsagnPerDeltaker ? "Ja" : "Nei"}
      />
      <PrismodellPrisbetingelser prisbetingelser={prismodell.prisbetingelser} />
    </VStack>
  );
}

function BetalingsbetingelserTilskudd({ prismodell }: PrismodellDetaljerProps) {
  const totalt = prismodell.tilskudd.reduce((acc, t) => t.belop + acc, 0);
  return (
    <VStack gap="space-8">
      <Heading size="xsmall">Tilskudd til en tilgjengelig studie- eller skoleplass</Heading>
      <BodyShort textColor="subtle">Utbetales basert på dokumenterte utgifter</BodyShort>
      <Heading size="xsmall">Aktuelle tilskuddstyper</Heading>
      <BodyShort textColor="subtle" spacing={true}>
        Ved flere semester er den estimerte totalsummen oppgitt
      </BodyShort>
      <List size="small" as="ul">
        {prismodell.tilskudd.map((t) => (
          <List.Item key={t.type}>
            <HStack justify="space-between">
              <BodyShort textColor="subtle" size="small">
                {opplaeringTilskuddToString(t.type)}
              </BodyShort>
              <BodyShort textColor="subtle" size="small">
                {formaterValuta(t.belop, prismodell.valuta)}
              </BodyShort>
            </HStack>
          </List.Item>
        ))}
      </List>
      <Separator />
      <BodyShort size="small" weight="semibold" className="ml-auto">
        {`Estimert totalsum: ${formaterValuta(totalt, prismodell.valuta)}`}
      </BodyShort>
      <Separator />
      {prismodell.prisbetingelser && (
        <>
          <Heading size="xsmall">Tilleggsopplysninger om kostnader</Heading>
          <BodyShort textColor="subtle">{prismodell.prisbetingelser}</BodyShort>
        </>
      )}
    </VStack>
  );
}

function BetalingsbetingelserIngenKostnader({ prismodell }: PrismodellDetaljerProps) {
  return (
    <VStack gap="space-8">
      <Heading size="xsmall">Ingen kostnader</Heading>
      <BodyShort textColor="subtle">Ikke aktuelt med betaling eller refusjon fra Nav</BodyShort>
      {prismodell.aarsak && (
        <>
          <Heading size="xsmall">
            Årsaken til at det ikke er aktuelt med betaling eller refusjon fra Nav
          </Heading>
          <BodyShort textColor="subtle">
            {ingenKostnaderAarsakToString(prismodell.aarsak)}
          </BodyShort>
        </>
      )}
      {prismodell.prisbetingelser && (
        <>
          <Heading size="xsmall">Tilleggsopplysninger om egenfinansieringen</Heading>
          <BodyShort textColor="subtle">{prismodell.prisbetingelser}</BodyShort>
        </>
      )}
    </VStack>
  );
}

function PrismodellTypenavn({ type }: { type: string }) {
  return <MetadataVStack label={avtaletekster.prismodell.label} value={type} />;
}

function PrismodellSatser({ satser }: { satser: AvtaltSatsDto[] | null }) {
  return (satser ?? []).map((sats) => (
    <Box
      key={sats.gjelderFra}
      borderColor="neutral-subtle"
      background="default"
      padding="space-8"
      borderWidth="1"
      borderRadius="4"
    >
      <HStack gap="space-16" key={sats.gjelderFra}>
        <MetadataVStack label={avtaletekster.prismodell.valuta.label} value={sats.pris.valuta} />
        <MetadataVStack
          label={avtaletekster.prismodell.sats.label}
          value={formaterValuta(sats.pris.belop, sats.pris.valuta)}
        />
        <MetadataVStack
          label={avtaletekster.prismodell.periodeStart.label}
          value={formaterDato(sats.gjelderFra)}
        />
        {sats.gjelderTil && (
          <MetadataVStack
            label={avtaletekster.prismodell.periodeSlutt.label}
            value={formaterDato(sats.gjelderTil)}
          />
        )}
      </HStack>
    </Box>
  ));
}

function PrismodellPrisbetingelser({ prisbetingelser }: { prisbetingelser: string | null }) {
  return <MetadataFritekstfelt label={avtaletekster.prisOgBetalingLabel} value={prisbetingelser} />;
}
