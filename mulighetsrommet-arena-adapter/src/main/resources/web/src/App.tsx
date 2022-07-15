import {
  Box,
  Button,
  Center,
  Flex,
  Heading,
  Spinner,
  Switch,
  Table,
  TableContainer,
  Tbody,
  Td,
  Th,
  Thead,
  Tr,
  VStack,
} from "@chakra-ui/react";
import type React from "react";
import { useEffect, useState } from "react";
import { getTopics } from "./api";
import { Layout } from "./components/Layout";
import { Topic } from "./domain";

function useTopics() {
  const [topics, setTopics] = useState<Topic[]>([]);
  const [isTopicsLoading, setIsLoading] = useState(true);
  useEffect(() => {
    const fetchedTopics = async () => {
      const t = await getTopics();
      setTopics(t);
      setIsLoading(false);
    };
    fetchedTopics();
  }, []);
  return { topics, isTopicsLoading, setTopics };
}

function putTopicRunningState(topics: Topic[]) {
  return fetch("http://localhost:8084/manager/topics", {
    method: "PUT",
    headers: {
      "content-type": "application/json",
    },
    body: JSON.stringify(topics),
  });
}

function TopicEnableDisable() {
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
    <Box>
      <Heading mb="4" size="lg">
        Topic overview
      </Heading>
      <Box boxShadow="sm" p="5" borderWidth="1px" rounded="md">
        {isTopicsLoading ? (
          <Box w="100%" minH="15rem">
            <Center h="15rem">
              <VStack>
                <Spinner thickness="4px" color="pink.500" size="xl" my="2" />
                <Heading size="sm">Fetching topics...</Heading>
              </VStack>
            </Center>
          </Box>
        ) : (
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
        )}
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
      </Box>
    </Box>
  );
}

function App() {
  return (
    <Layout>
      <TopicEnableDisable />
    </Layout>
  );
}

export default App;
