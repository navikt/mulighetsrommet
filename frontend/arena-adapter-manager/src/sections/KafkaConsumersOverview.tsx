import { ChangeEvent, useState } from "react";
import { Section } from "../components/Section";
import { ApiBase, putKafkaConsumerRunningState } from "../core/api";
import { useKafkaConsumers } from "../core/hooks";
import { Table, Switch, Button, HStack } from "@navikt/ds-react";

interface Props {
  base: ApiBase;
}

function KafkaConsumersOverview({ base }: Props) {
  const { consumers, isConsumersLoading, setConsumers } = useKafkaConsumers(base);
  const [isSaveLoading, setIsSaveLoading] = useState(false);

  const setRunningState = (event: ChangeEvent<HTMLInputElement>) => {
    const changedConsumers = [...consumers];
    changedConsumers[changedConsumers.map((c) => c.id).indexOf(event.currentTarget.name)].running =
      event.currentTarget.checked;
    setConsumers(changedConsumers);
  };

  const saveRunningState = async () => {
    setIsSaveLoading(true);
    await putKafkaConsumerRunningState(base, consumers);
    setIsSaveLoading(false);
  };

  return (
    <Section
      headerText="Kafka consumers"
      isLoading={isConsumersLoading}
      loadingText="Fetching Kafka consumers..."
    >
      <Table>
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell>Consumer ID</Table.HeaderCell>
            <Table.HeaderCell>Topic</Table.HeaderCell>
            <Table.HeaderCell>Running</Table.HeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {consumers.map((consumer) => (
            <Table.Row key={consumer.id}>
              <Table.DataCell>{consumer.id}</Table.DataCell>
              <Table.DataCell>
                <strong>{consumer.topic}</strong>
              </Table.DataCell>
              <Table.DataCell>
                <Switch
                  name={consumer.id}
                  defaultChecked={consumer.running}
                  onChange={setRunningState}
                  size="medium"
                  hideLabel
                >
                  Consumer running
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

export default KafkaConsumersOverview;
