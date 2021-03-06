import {
  TableContainer,
  Table,
  Thead,
  Tr,
  Th,
  Tbody,
  Td,
  Switch,
  Flex,
  Button,
  Box,
} from "@chakra-ui/react";
import { useState } from "react";
import { Section } from "../components/Section";
import { putTopicRunningState } from "../core/api";
import { useTopics } from "../core/hooks";

function TopicOverview() {
  const { topics, isTopicsLoading, setTopics } = useTopics();
  const [isSaveLoading, setIsSaveLoading] = useState(false);

  const setRunningState = (event: React.ChangeEvent<HTMLInputElement>) => {
    const changedTopics = [...topics];
    changedTopics[
      changedTopics.map((t) => t.topic).indexOf(event.currentTarget.name)
    ].running = event.currentTarget.checked;
    setTopics(changedTopics);
  };

  const saveRunningState = async () => {
    setIsSaveLoading(true);
    await putTopicRunningState(topics).then();
    setIsSaveLoading(false);
  };

  return (
    <Section
      headerText="Topic overview"
      isLoading={isTopicsLoading}
      loadingText="Fetching topics..."
    >
      <TableContainer>
        <Table>
          <Thead>
            <Tr>
              <Th>Key</Th>
              <Th>Type</Th>
              <Th>Topic</Th>
              <Th>Running</Th>
            </Tr>
          </Thead>
          <Tbody>
            {topics.map((topic) => (
              <Tr key={topic.id}>
                <Td>{topic.key}</Td>
                <Td>{topic.type}</Td>
                <Td>
                  <strong>{topic.topic}</strong>
                </Td>
                <Td>
                  <Switch
                    colorScheme="pink"
                    name={topic.topic}
                    defaultChecked={topic.running}
                    onChange={setRunningState}
                    size="lg"
                  />
                </Td>
              </Tr>
            ))}
          </Tbody>
        </Table>
      </TableContainer>
      <Box mt="6">
        <Flex justifyContent="end">
          <Button
            isLoading={isSaveLoading}
            colorScheme="pink"
            onClick={saveRunningState}
          >
            Save
          </Button>
        </Flex>
      </Box>
    </Section>
  );
}

export default TopicOverview;
