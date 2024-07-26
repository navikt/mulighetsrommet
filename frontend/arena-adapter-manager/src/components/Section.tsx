import { Box, Center, Heading, Spinner, VStack } from "@chakra-ui/react";
import { PropsWithChildren } from "react";

interface SectionProps extends PropsWithChildren {
  headerText: string;
  isLoading: boolean;
  loadingText: string;
}

export function Section({ children, headerText, isLoading, loadingText }: SectionProps) {
  return (
    <Box w="100%">
      <Box rounded={6} p="2" bg="pink" border="1px" borderColor="pink.300" mb="2" w="fit-content">
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
}
