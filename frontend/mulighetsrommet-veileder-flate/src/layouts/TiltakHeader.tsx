import { VeilederflateTiltak } from "@api-client";
import { isTiltakAktivt, isTiltakGruppe } from "@/api/queries/useArbeidsmarkedstiltakById";
import { BodyLong, BodyShort, Box, Heading, HStack, Table, VStack } from "@navikt/ds-react";
import { StatusTag } from "@mr/frontend-common";
import { formaterDato } from "@/utils/Utils";

interface Props {
  tiltak: VeilederflateTiltak;
}

export function TiltakHeader({ tiltak }: Props) {
  const { beskrivelse, tiltakstype } = tiltak;
  const stengtPerioder = "stengtPerioder" in tiltak ? tiltak.stengtPerioder : null;
  return (
    <VStack>
      <BodyShort spacing size="small">
        {tiltak.tiltakstype.navn}
      </BodyShort>
      <HStack gap="space-16" align="center">
        <Heading size="large" spacing>
          {tiltak.navn}
        </Heading>
        {isTiltakGruppe(tiltak) && !isTiltakAktivt(tiltak) && (
          <StatusTag dataColor="neutral">{tiltak.status.beskrivelse}</StatusTag>
        )}
      </HStack>
      {beskrivelse && (
        <BodyLong size="large" spacing>
          {beskrivelse}
        </BodyLong>
      )}
      {tiltakstype.beskrivelse && (
        <>
          <Heading level="2" size="small">
            Generell informasjon
          </Heading>
          <BodyLong size="large" spacing>
            {tiltakstype.beskrivelse}
          </BodyLong>
        </>
      )}

      {stengtPerioder && (
        <>
          <Heading level="4" size="xsmall" spacing>
            Perioder hvor tiltakstilbudet er stengt hos arrangør
          </Heading>
          <Box
            background="neutral-soft"
            padding="space-8"
            borderColor="neutral"
            borderWidth="1"
            borderRadius="8"
            marginBlock="space-0 space-16"
          >
            <Table>
              <Table.Header>
                <Table.Row>
                  <Table.HeaderCell textSize="small">Periode</Table.HeaderCell>
                  <Table.HeaderCell textSize="small">Beskrivelse</Table.HeaderCell>
                </Table.Row>
              </Table.Header>
              <Table.Body>
                {stengtPerioder.map((periode) => {
                  return (
                    <Table.Row key={periode.id}>
                      <Table.DataCell textSize="small">{`${formaterDato(periode.start)} - ${formaterDato(periode.slutt)}`}</Table.DataCell>
                      <Table.DataCell textSize="small">{periode.beskrivelse}</Table.DataCell>
                    </Table.Row>
                  );
                })}
              </Table.Body>
            </Table>
          </Box>
        </>
      )}
    </VStack>
  );
}
