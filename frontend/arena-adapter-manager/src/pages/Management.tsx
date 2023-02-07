import {Box, Heading, VStack} from "@chakra-ui/react";
import ReplayEvents from "../sections/ReplayEvents";
import TopicOverview from "../sections/TopicOverview";
import ReplayEvent from "../sections/ReplayEvent";
import DeleteEvents from "../sections/DeleteEvents";

function Management() {
    return (
        <Box>
            <Heading mb="10">Management</Heading>
            <VStack spacing={8}>
                <TopicOverview/>
                <ReplayEvents/>
                <ReplayEvent/>
                <DeleteEvents/>
            </VStack>
        </Box>
    );
}

export default Management;
