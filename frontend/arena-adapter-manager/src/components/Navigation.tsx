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
import { Link, useLocation } from "react-router";
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

interface Props {
  apps: { name: string; path: string }[];
}

function Navigation({ apps }: Props) {
  return (
    <Box w="inherit" bg="pink.600">
      <Container pt="8" maxW="container.xl">
        <Flex w="100%">
          <LinkBox mb="8" w="fit-content" mr="10">
            <LinkOverlay color="white" _hover={{ color: "pink.500" }} href="/">
              <Heading size="4xl">MAAM</Heading>
            </LinkOverlay>
            <Heading color="pink.300" size="xs">
              mulighetsrommet-arena-adapter-manager
            </Heading>
          </LinkBox>
          <Box flex="auto" p="5" rounded="md">
            <HStack spacing={4}>
              {apps.map((app) => (
                <NavButton key={app.name} to={app.path} icon={AiOutlineSetting}>
                  {app.name}
                </NavButton>
              ))}
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
