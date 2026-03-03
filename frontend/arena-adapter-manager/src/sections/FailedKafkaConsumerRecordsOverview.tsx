import { KafkaConsumerRecord } from "src/domain";
import { Section } from "../components/Section";
import { ApiBase, putRetryFailedKafkaRecord } from "../core/api";
import { useFailedKafkaConsumerRecords } from "../core/hooks";
import { BodyShort, Box, Button, HStack, Label, Table, VStack } from "@navikt/ds-react";
import { formatUTCDate } from "../utils";
import { PencilIcon } from "@navikt/aksel-icons";
import { ExecutionTimeModalForm } from "../components/forms/ExecutionTimeModalForm";
import { useState } from "react";

interface Props {
  base: ApiBase;
}

interface ExecutionTimeData {
  id: number;
  topic: string;
  lastRetry: Date;
}

function FailedKafkaConsumerRecordsOverview({ base }: Props) {
  const { records, isLoading, refetch } = useFailedKafkaConsumerRecords(base);
  const [data, setData] = useState<ExecutionTimeData | null>(null);

  async function handleSubmit(previousData: ExecutionTimeData, newTime: Date) {
    setData(null);
    await putRetryFailedKafkaRecord(base, {
      id: previousData.id,
      topic: previousData.topic,
      executionTime: newTime,
    });
    refetch();
  }

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
              <Table.HeaderCell></Table.HeaderCell>
              <Table.HeaderCell></Table.HeaderCell>
              <Table.HeaderCell>Id</Table.HeaderCell>
              <Table.HeaderCell>Topic</Table.HeaderCell>
              <Table.HeaderCell>Retries</Table.HeaderCell>
              <Table.HeaderCell>Last retry</Table.HeaderCell>
              <Table.HeaderCell></Table.HeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {records.map((record) => {
              const lastRetry = formatUTCDate(record.lastRetry);
              return (
                <Table.ExpandableRow
                  togglePlacement="left"
                  key={record.id}
                  content={<ExpandedRow record={record} />}
                >
                  <Table.DataCell>
                    <Button
                      icon={<PencilIcon title="Rediger" />}
                      onClick={() =>
                        setData({
                          id: record.id,
                          topic: record.topic,
                          lastRetry: new Date(lastRetry),
                        })
                      }
                    />
                  </Table.DataCell>
                  <Table.DataCell>{record.id}</Table.DataCell>
                  <Table.DataCell>{record.topic}</Table.DataCell>
                  <Table.DataCell>{record.retries}</Table.DataCell>
                  <Table.DataCell>{lastRetry}</Table.DataCell>
                </Table.ExpandableRow>
              );
            })}
          </Table.Body>
        </Table>
      )}
      {data && (
        <ExecutionTimeModalForm
          initDate={data.lastRetry}
          onSubmit={(executionTime) => handleSubmit(data, executionTime)}
          onClose={() => setData(null)}
          displayData={{ Id: data.id.toString(), Topic: data.topic }}
        />
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
          <Label size="small">Partition</Label>
          <BodyShort>{record.partition}</BodyShort>
        </Box>
        <Box padding="space-6">
          <Label size="small">Record offset</Label>
          <BodyShort>{record.recordOffset}</BodyShort>
        </Box>
        <Box padding="space-6">
          <Label size="small">Created at</Label>
          <BodyShort>{formatUTCDate(record.createdAt)}</BodyShort>
        </Box>
        <Box padding="space-6">
          <Label size="small">Record timestamp</Label>
          <BodyShort>{record.recordTimestamp}</BodyShort>
        </Box>
      </HStack>
      <Box background="sunken" padding="space-4">
        <Label size="small">Key</Label>
        <pre>{record.key}</pre>
      </Box>
      <Box background="sunken" padding="space-4">
        <Label className="font-bold">Value</Label>
        <pre>{record.value}</pre>
      </Box>
    </VStack>
  );
}

export default FailedKafkaConsumerRecordsOverview;
