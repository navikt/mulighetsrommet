import {
  Box,
  Container,
  FormControl,
  FormLabel,
  Heading,
  LinkBox,
  LinkOverlay,
  SimpleGrid,
  Stack,
  Switch,
  Text,
} from "@chakra-ui/react";
import type React from "react";
import { Fragment } from "react";

const Layout = ({ children }: React.PropsWithChildren) => (
  <Box as="main" w="100%">
    <Container mt="8" maxW="container.lg">
      <LinkBox mb="8">
        <LinkOverlay href="https://youtu.be/RJHctyXPmkg?t=5">
          <Heading size="4xl">MAAM</Heading>
        </LinkOverlay>
        <Heading size="xs">mulighetsrommet-arena-adapter-manager</Heading>
      </LinkBox>
      {children}
    </Container>
  </Box>
);

function TopicEnableDisable() {
  // TODO: Bytt ut med fetch
  const topics = [
    "teamarenanais.aapen-arena-tiltakgjennomforingendret-v1-q2",
    "teamarenanais.aapen-arena-tiltakdeltakerendret-v1-q2",
    "teamarenanais.aapen-arena-tiltakendret-v1-q2",
    "teamarenanais.aapen-arena-sakendret-v1-q2",
  ];
  return (
    <Box>
      <Heading mb="4">Topic control</Heading>
      <Box boxShadow="sm" p="5" borderWidth="1px" rounded="md">
        <FormControl as={SimpleGrid} columns={{ base: 0, lg: 2 }} spacing={2}>
          {topics.map((topic) => (
            <Fragment key={topic}>
              <FormLabel>{topic}</FormLabel>
              <Switch size="lg" />
            </Fragment>
          ))}
        </FormControl>
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
