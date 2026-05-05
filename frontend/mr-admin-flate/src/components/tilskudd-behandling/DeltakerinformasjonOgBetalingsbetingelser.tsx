import { BodyShort, Box, Heading, InlineMessage, Table, VStack } from "@navikt/ds-react";
import { DataElementStatusTag } from "@mr/frontend-common";
import { DeltakerDto } from "@tiltaksadministrasjon/api-client";

interface Props {
  deltaker: DeltakerDto | null;
  prisbetingelser: string | null;
}

export function DeltakerinformasjonOgBetalingsbetingelser({ deltaker, prisbetingelser }: Props) {
  if (!deltaker) {
    return <InlineMessage status="error">Fant ikke deltakerinformasjon</InlineMessage>;
  }

  return (
    <VStack gap="space-16">
      <section>
        <Heading size="small" level="3" spacing>
          Deltakerinformasjon
        </Heading>
        <Table>
          <Table.Header>
            <Table.Row>
              <Table.HeaderCell scope="col">Deltaker og enhet</Table.HeaderCell>
              <Table.HeaderCell scope="col">Status</Table.HeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            <Table.Row>
              <Table.DataCell>
                <VStack>
                  <BodyShort>{deltaker.navn}</BodyShort>
                  <BodyShort>{deltaker.norskIdent}</BodyShort>
                  <BodyShort>{deltaker.oppfolgingEnhet?.navn}</BodyShort>
                </VStack>
              </Table.DataCell>
              <Table.DataCell>
                <DataElementStatusTag {...deltaker.status} />
              </Table.DataCell>
            </Table.Row>
          </Table.Body>
        </Table>
      </section>
      <section>
        <Box background="neutral-soft" padding="space-16" borderRadius="8">
          <BodyShort weight="semibold">Pris og betalingsbetingelser:</BodyShort>
          <BodyShort>{prisbetingelser}</BodyShort>
        </Box>
      </section>
    </VStack>
  );
}
