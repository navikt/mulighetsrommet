import { VStack } from "@chakra-ui/react";
import { Layout } from "./components/Layout";
import TopicOverview from "./sections/TopicOverview";

function App() {
  return (
    <Layout>
      <VStack spacing={8}>
        <TopicOverview />
      </VStack>
    </Layout>
  );
}

export default App;
