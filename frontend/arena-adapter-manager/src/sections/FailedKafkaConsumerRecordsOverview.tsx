import { KafkaConsumerRecord } from "src/domain";
import { JsonOrTextValue } from "../components/JsonOrTextValue";
import { Section } from "../components/Section";
import { ApiBase, deleteFailedKafkaRecord, putRetryFailedKafkaRecord } from "../core/api";
import { useFailedKafkaConsumerRecords } from "../core/hooks";
import { BodyShort, Box, Button, HStack, Label, Table, VStack } from "@navikt/ds-react";
import { formatUTCDate, isDevelopment } from "../utils";
import { PencilIcon, TrashIcon } from "@navikt/aksel-icons";
import { DeleteConfirmationModal } from "../components/DeleteConfirmationModal";
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
  const [recordToDelete, setRecordToDelete] = useState<KafkaConsumerRecord | null>(null);

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
              <Table.HeaderCell>Delete</Table.HeaderCell>
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
                  <Table.DataCell>
                    <Button
                      variant="secondary"
                      data-color="danger"
                      disabled={!isDevelopment()}
                      icon={<TrashIcon title="Delete Kafka record" />}
                      onClick={() => setRecordToDelete(record)}
                    />
                  </Table.DataCell>
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
      {recordToDelete && (
        <DeleteConfirmationModal
          title="Delete Kafka record?"
          description={`Delete record ${recordToDelete.id} from ${recordToDelete.topic}?`}
          onConfirm={async () => {
            const deleted = await deleteFailedKafkaRecord(base, recordToDelete);
            if (deleted) refetch();
            return deleted;
          }}
          onClose={() => setRecordToDelete(null)}
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
        <JsonOrTextValue value={record.key} />
      </Box>
      <Box background="sunken" padding="space-4">
        <Label className="font-bold">Value</Label>
        <JsonOrTextValue value={record.value} />
      </Box>
    </VStack>
  );
}

export default FailedKafkaConsumerRecordsOverview;
