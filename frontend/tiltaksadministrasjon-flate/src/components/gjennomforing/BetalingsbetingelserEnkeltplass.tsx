import { ingenKostnaderAarsakToString, opplaeringTilskuddToString } from "@/utils/Utils";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { formaterValuta } from "@mr/frontend-common/utils/utils";
import { BodyShort, Heading, HStack, List, VStack } from "@navikt/ds-react";
import { PrismodellDto, PrismodellType, Valuta } from "@tiltaksadministrasjon/api-client";

interface Props {
  prismodell: PrismodellDto;
}

export function BetalingsbetingelserEnkeltplass({ prismodell }: Props) {
  switch (prismodell.type) {
    case PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK:
    case PrismodellType.FORHANDSGODKJENT_PRIS_PER_AVTALT_TILTAKSPLASS:
    case PrismodellType.AVTALT_PRIS_PER_MANEDSVERK:
    case PrismodellType.AVTALT_PRIS_PER_UKESVERK:
    case PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK:
    case PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER:
    case PrismodellType.ANNEN_AVTALT_PRIS:
      return <BetalingsbetingelserAnskaffelse prismodell={prismodell} />;
    case PrismodellType.TILSKUDD_TIL_OPPLAERING:
      return <BetalingsbetingelserTilskudd prismodell={prismodell} />;
    case PrismodellType.INGEN_KOSTNADER:
      return <BetalingsbetingelserIngenKostnader prismodell={prismodell} />;
  }
}

function BetalingsbetingelserAnskaffelse({ prismodell }: Props) {
  return (
    <VStack gap="space-8">
      <Heading size="small">Pris- og betalingsbetingelser</Heading>
      <Heading size="xsmall">Anskaffelse</Heading>
      <BodyShort textColor="subtle">Nav har avtalt å betale leverandøren direkte</BodyShort>
      <Heading size="xsmall">Totalbeløp for anskaffelsen</Heading>
      <BodyShort textColor="subtle">
        {prismodell.totalBelop ? formaterValuta(prismodell.totalBelop, Valuta.NOK) : "-"}
      </BodyShort>
    </VStack>
  );
}

function BetalingsbetingelserTilskudd({ prismodell }: Props) {
  return (
    <VStack gap="space-8">
      <Heading size="small">Pris- og betalingsbetingelser</Heading>
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
                {formaterValuta(t.belop, Valuta.NOK)}
              </BodyShort>
            </HStack>
          </List.Item>
        ))}
      </List>
      <Separator />
      <BodyShort
        size="small"
        weight="semibold"
        className="ml-auto"
      >{`Estimert totalsum: ${formaterValuta(
        prismodell.tilskudd.reduce((acc, t) => t.belop + acc, 0),
        Valuta.NOK,
      )}`}</BodyShort>
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

function BetalingsbetingelserIngenKostnader({ prismodell }: Props) {
  return (
    <VStack gap="space-8">
      <Heading size="small">Pris- og betalingsbetingelser</Heading>
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
