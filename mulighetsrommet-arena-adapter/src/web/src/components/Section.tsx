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
    <Heading mb="4" size="lg">
      {headerText}
    </Heading>
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
