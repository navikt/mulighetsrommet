import {
  Box,
  Button,
  Container,
  Flex,
  Heading,
  HStack,
  Icon,
  Image,
  LinkBox,
  LinkOverlay,
} from "@chakra-ui/react";
import { AiOutlineSetting } from "react-icons/ai";
import { Link, useLocation } from "react-router-dom";
import snoop from "../images/snoop.gif";
import { IconType } from "react-icons";

function NavButton({ children, icon, to }: { children: string; icon: IconType; to: string }) {
  const location = useLocation();
  return (
    <Link to={to}>
      <Button
        leftIcon={<Icon as={icon} />}
        colorScheme="whiteAlpha"
        size="lg"
        variant={location.pathname === to ? "solid" : "ghost"}
      >
        {children}
      </Button>
    </Link>
  );
}

function Navigation() {
  return (
    <Box w="inherit" bg="pink.600">
      <Container pt="8" maxW="container.xl">
        <Flex w="100%">
          <LinkBox mb="8" w="fit-content" mr="10">
            <LinkOverlay
              color="white"
              _hover={{ color: "pink.500" }}
              href="https://youtu.be/RJHctyXPmkg?t=5"
            >
              <Heading size="4xl">MAAM</Heading>
            </LinkOverlay>
            <Heading color="pink.300" size="xs">
              mulighetsrommet-arena-adapter-manager
            </Heading>
          </LinkBox>
          <Box flex="auto" p="5" rounded="md">
            <HStack spacing={4}>
              <NavButton to="/mr-arena-adapter" icon={AiOutlineSetting}>
                mr-arena-adapter
              </NavButton>
              <NavButton to="/mr-api" icon={AiOutlineSetting}>
                mr-api
              </NavButton>
            </HStack>
          </Box>
          <Box>
            <Image w="130px" src={snoop} alt="snoop dogg" objectFit="cover" />
          </Box>
        </Flex>
      </Container>
    </Box>
  );
}

export default Navigation;
