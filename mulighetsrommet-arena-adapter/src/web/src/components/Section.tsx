import { Heading, Center, VStack, Spinner, Box } from "@chakra-ui/react";

interface SectionProps extends React.PropsWithChildren {
  headerText: string;
  isLoading: boolean;
  loadingText: string;
}

export const Section = ({
  children,
  headerText,
  isLoading,
  loadingText,
}: SectionProps) => (
  <Box w="100%">
    <Box
      rounded={6}
      p="2"
      bg="pink"
      border="1px"
      borderColor="pink.300"
      mb="2"
      w="fit-content"
    >
      <Heading size="md">{headerText}</Heading>
    </Box>
    <Box boxShadow="sm" p="5" borderWidth="1px" rounded="md">
      {isLoading ? (
        <Box w="100%" minH="15rem">
          <Center h="15rem">
            <VStack>
              <Spinner thickness="4px" color="pink.500" size="xl" my="2" />
              <Heading size="sm">{loadingText}</Heading>
            </VStack>
          </Center>
        </Box>
      ) : (
        children
      )}
    </Box>
  </Box>
);
