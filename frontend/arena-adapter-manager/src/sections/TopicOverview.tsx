import { ChangeEvent, useState } from "react";
import { Section } from "../components/Section";
import { ApiBase, putTopicRunningState } from "../core/api";
import { useTopics } from "../core/hooks";
import { Table, Switch, Button, HStack } from "@navikt/ds-react";

interface Props {
  base: ApiBase;
}

function TopicOverview({ base }: Props) {
  const { topics, isTopicsLoading, setTopics } = useTopics(base);
  const [isSaveLoading, setIsSaveLoading] = useState(false);

  const setRunningState = (event: ChangeEvent<HTMLInputElement>) => {
    const changedTopics = [...topics];
    changedTopics[changedTopics.map((t) => t.id).indexOf(event.currentTarget.name)].running =
      event.currentTarget.checked;
    setTopics(changedTopics);
  };

  const saveRunningState = async () => {
    setIsSaveLoading(true);
    await putTopicRunningState(base, topics);
    setIsSaveLoading(false);
  };

  return (
    <Section
      headerText="Topic overview"
      isLoading={isTopicsLoading}
      loadingText="Fetching topics..."
    >
      <Table>
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell>Key</Table.HeaderCell>
            <Table.HeaderCell>Type</Table.HeaderCell>
            <Table.HeaderCell>Topic</Table.HeaderCell>
            <Table.HeaderCell>Running</Table.HeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {topics.map((topic) => (
            <Table.Row key={topic.id}>
              <Table.DataCell>{topic.id}</Table.DataCell>
              <Table.DataCell>{topic.type}</Table.DataCell>
              <Table.DataCell>
                <strong>{topic.topic}</strong>
              </Table.DataCell>
              <Table.DataCell>
                <Switch
                  name={topic.id}
                  defaultChecked={topic.running}
                  onChange={setRunningState}
                  size="medium"
                  hideLabel
                >
                  Topic running
                </Switch>
              </Table.DataCell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table>
      <HStack justify="end">
        <Button loading={isSaveLoading} onClick={saveRunningState}>
          Save
        </Button>
      </HStack>
    </Section>
  );
}

export default TopicOverview;
