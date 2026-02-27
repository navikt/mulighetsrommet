import { ScheduledTask } from "src/domain";
import { Section } from "../components/Section";
import { ApiBase } from "../core/api";
import { useFailedScheduledTasks } from "../core/hooks";
import { BodyShort, Box, Heading, HStack, Table, VStack } from "@navikt/ds-react";
import { formatUTCDate } from "../utils";

interface Props {
  base: ApiBase;
}

function FailedScheduledTasksOverview({ base }: Props) {
  const { tasks, isLoading } = useFailedScheduledTasks(base);

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
              <Table.HeaderCell>Name</Table.HeaderCell>
              <Table.HeaderCell>Instance</Table.HeaderCell>
              <Table.HeaderCell>Consecutive failures</Table.HeaderCell>
              <Table.HeaderCell>Next execution time</Table.HeaderCell>
              <Table.HeaderCell></Table.HeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {tasks.map((task) => (
              <Table.ExpandableRow
                togglePlacement="right"
                key={task.taskInstance}
                content={<ExpandedRow task={task} />}
              >
                <Table.DataCell>{task.taskName}</Table.DataCell>
                <Table.DataCell>{task.taskInstance}</Table.DataCell>
                <Table.DataCell>{task.consecutiveFailures}</Table.DataCell>
                <Table.DataCell>{formatUTCDate(task.executionTime)}</Table.DataCell>
              </Table.ExpandableRow>
            ))}
          </Table.Body>
        </Table>
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
          <Heading size="xsmall">Last Failure</Heading>
          <BodyShort>{formatUTCDate(task.lastFailure)}</BodyShort>
        </Box>
        <Box padding="space-6">
          <Heading size="xsmall">Picked</Heading>
          <BodyShort>{task.picked}</BodyShort>
        </Box>
        <Box padding="space-6">
          <Heading size="xsmall">Picked by</Heading>
          <BodyShort>{task.pickedBy}</BodyShort>
        </Box>
      </HStack>
      <Box background="sunken" padding="space-4">
        <Heading size="small">Data</Heading>
        <pre>{task.taskData}</pre>
      </Box>
    </VStack>
  );
}

export default FailedScheduledTasksOverview;
