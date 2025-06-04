import { Button, Checkbox, Heading, HStack, VStack } from "@navikt/ds-react";
import { Form, MetaFunction, Link as ReactRouterLink } from "react-router";
import { internalNavigation } from "~/internal-navigation";
import { useOrgnrFromUrl } from "~/utils";
import { Definisjonsliste } from "../components/Definisjonsliste";
import { tekster } from "../tekster";

export const meta: MetaFunction = () => {
  return [
    { title: "Bekreft utbetaling" },
    { name: "description", content: "Arrangørflate for bekreftelse av krav om utbetaling" },
  ];
};

export default function BekreftUtbetaling() {
  const orgnr = useOrgnrFromUrl();

  return (
    <>
      <Heading level="2" spacing size="large">
        Oppsummering
      </Heading>
      <Form>
        <VStack gap="6">
          <Definisjonsliste
            title="Innsendingsinformasjon"
            headingLevel="3"
            definitions={[
              {
                key: "Arrangør",
                value: orgnr,
              },
              { key: "Tiltaksnavn", value: "mock" },
              { key: "Tiltakstype", value: "mock" },
            ]}
          />
          <Checkbox name="bekreftelse" value="bekreftet" id="bekreftelse">
            {tekster.bokmal.utbetaling.oppsummering.bekreftelse}
          </Checkbox>
          <HStack gap="4">
            <Button
              as={ReactRouterLink}
              type="button"
              variant="tertiary"
              to={internalNavigation(orgnr).opprettKravUtbetaling}
            >
              Tilbake
            </Button>
            <Button type="submit">Bekreft og send inn</Button>
          </HStack>
        </VStack>
      </Form>
    </>
  );
}
