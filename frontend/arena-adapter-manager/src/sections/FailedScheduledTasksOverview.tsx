import { ScheduledTask } from "src/domain";
import { Section } from "../components/Section";
import { ApiBase, putRetryScheduledTask } from "../core/api";
import { useFailedScheduledTasks } from "../core/hooks";
import { BodyShort, Box, Button, HStack, Label, Table, VStack } from "@navikt/ds-react";
import { formatUTCDate } from "../utils";
import { useState } from "react";
import { ExecutionTimeModalForm } from "../components/forms/ExecutionTimeModalForm";
import { PencilIcon } from "@navikt/aksel-icons";

interface Props {
  base: ApiBase;
}

interface ExecutionTimeData {
  taskName: string;
  taskInstance: string;
  executionTime: Date;
}

function FailedScheduledTasksOverview({ base }: Props) {
  const { tasks, isLoading, refetch } = useFailedScheduledTasks(base);
  const [data, setData] = useState<ExecutionTimeData | null>(null);

  async function handleSubmit(previousData: ExecutionTimeData, newTime: Date) {
    setData(null);
    await putRetryScheduledTask(base, {
      taskName: previousData.taskName,
      taskInstance: previousData.taskInstance,
      executionTime: newTime,
    });
    refetch();
  }

  return (
    <Section
      headerText="Failed scheduled tasks"
      isLoading={isLoading}
      loadingText="Fetching tasks..."
    >
      {tasks.length === 0 ? (
        <p>No failed tasks 🎉</p>
      ) : (
        <Table>
          <Table.Header>
            <Table.Row>
              <Table.HeaderCell></Table.HeaderCell>
              <Table.HeaderCell></Table.HeaderCell>
              <Table.HeaderCell>Name</Table.HeaderCell>
              <Table.HeaderCell>Instance</Table.HeaderCell>
              <Table.HeaderCell>Consecutive failures</Table.HeaderCell>
              <Table.HeaderCell>Next execution time</Table.HeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {tasks.map((task) => {
              const executionTime = formatUTCDate(task.executionTime);
              return (
                <Table.ExpandableRow
                  togglePlacement="left"
                  key={task.taskInstance}
                  content={<ExpandedRow task={task} />}
                >
                  <Table.DataCell>
                    <Button
                      icon={<PencilIcon title="Rediger" />}
                      onClick={() =>
                        setData({
                          taskName: task.taskName,
                          taskInstance: task.taskInstance,
                          executionTime: new Date(executionTime),
                        })
                      }
                    />
                  </Table.DataCell>
                  <Table.DataCell>{task.taskName}</Table.DataCell>
                  <Table.DataCell>{task.taskInstance}</Table.DataCell>
                  <Table.DataCell>{task.consecutiveFailures}</Table.DataCell>
                  <Table.DataCell>{formatUTCDate(task.executionTime)}</Table.DataCell>
                </Table.ExpandableRow>
              );
            })}
          </Table.Body>
        </Table>
      )}
      {data && (
        <ExecutionTimeModalForm
          initDate={data.executionTime}
          onSubmit={(executionTime) => handleSubmit(data, executionTime)}
          onClose={() => setData(null)}
          displayData={{ "Task name": data.taskName, "Task instance": data.taskInstance }}
        />
      )}
    </Section>
  );
}

interface ExpandedRowProps {
  task: ScheduledTask;
}

function ExpandedRow({ task }: ExpandedRowProps) {
  return (
    <VStack gap="space-8">
      <HStack gap="space-4" justify="space-between">
        <Box padding="space-6">
          <Label size="small">Last Failure</Label>
          <BodyShort>{formatUTCDate(task.lastFailure)}</BodyShort>
        </Box>
        <Box padding="space-6">
          <Label size="small">Picked</Label>
          <BodyShort>{task.picked}</BodyShort>
        </Box>
        <Box padding="space-6">
          <Label size="small">Picked by</Label>
          <BodyShort>{task.pickedBy}</BodyShort>
        </Box>
      </HStack>
      <Box background="sunken" padding="space-4">
        <Label size="small">Data</Label>
        <pre>{task.taskData}</pre>
      </Box>
    </VStack>
  );
}

export default FailedScheduledTasksOverview;
