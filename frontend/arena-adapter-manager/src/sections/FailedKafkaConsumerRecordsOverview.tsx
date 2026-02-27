import { KafkaConsumerRecord } from "src/domain";
import { Section } from "../components/Section";
import { ApiBase } from "../core/api";
import { useFailedKafkaConsumerRecords } from "../core/hooks";
import { BodyShort, Box, Heading, HStack, Table, VStack } from "@navikt/ds-react";
import { formatUTCDate } from "../utils";

interface Props {
  base: ApiBase;
}

function FailedKafkaConsumerRecordsOverview({ base }: Props) {
  const { records, isLoading } = useFailedKafkaConsumerRecords(base);

  return (
    <Section
      headerText="Failed kafka consumer records"
      isLoading={isLoading}
      loadingText="Fetching records..."
    >
      {records.length === 0 ? (
        <p>No failed records 🎉</p>
      ) : (
        <Table>
          <Table.Header>
            <Table.Row>
              <Table.HeaderCell>Id</Table.HeaderCell>
              <Table.HeaderCell>Topic</Table.HeaderCell>
              <Table.HeaderCell>Retries</Table.HeaderCell>
              <Table.HeaderCell>Last retry</Table.HeaderCell>
              <Table.HeaderCell></Table.HeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {records.map((record) => (
              <Table.ExpandableRow
                togglePlacement="right"
                key={record.id}
                content={<ExpandedRow record={record} />}
              >
                <Table.DataCell>{record.id}</Table.DataCell>
                <Table.DataCell>{record.topic}</Table.DataCell>
                <Table.DataCell>{record.retries}</Table.DataCell>
                <Table.DataCell>{formatUTCDate(record.lastRetry)}</Table.DataCell>
              </Table.ExpandableRow>
            ))}
          </Table.Body>
        </Table>
      )}
    </Section>
  );
}

interface ExpandedRowProps {
  record: KafkaConsumerRecord;
}

function ExpandedRow({ record }: ExpandedRowProps) {
  return (
    <VStack gap="space-8">
      <HStack gap="space-4" justify="space-between">
        <Box padding="space-6">
          <Heading size="xsmall">Partition</Heading>
          <BodyShort>{record.partition}</BodyShort>
        </Box>
        <Box padding="space-6">
          <Heading size="xsmall">Record offset</Heading>
          <BodyShort>{record.recordOffset}</BodyShort>
        </Box>
        <Box padding="space-6">
          <Heading size="xsmall">Created at</Heading>
          <BodyShort>{formatUTCDate(record.createdAt)}</BodyShort>
        </Box>
        <Box padding="space-6">
          <Heading size="xsmall">Record timestamp</Heading>
          <BodyShort>{formatUTCDate(record.recordTimestamp)}</BodyShort>
        </Box>
      </HStack>
      <Box background="sunken" padding="space-4">
        <Heading size="small">Key</Heading>
        <pre>{record.key}</pre>
      </Box>
      <Box background="sunken" padding="space-4">
        <Heading size="small">Value</Heading>
        <pre>{record.value}</pre>
      </Box>
    </VStack>
  );
}

export default FailedKafkaConsumerRecordsOverview;
