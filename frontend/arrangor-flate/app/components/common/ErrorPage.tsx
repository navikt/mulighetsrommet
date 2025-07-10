import { BodyShort, Box, Heading, Button, Alert, Page } from "@navikt/ds-react";
import { NavigateFunction } from "react-router";

type Props = {
  heading: string;
  body: string[];
  navigate: NavigateFunction;
};

export function ErrorPage(props: Props) {
  return (
    <Page className="min-h-[calc(100vh-200px)] flex items-center justify-center">
      <Box padding="8" className="max-w-[600px] w-full mx-auto text-center">
        <Alert variant="error" className="mb-[2rem]">
          <Heading spacing size="large" level="2">
            {props.heading}
          </Heading>
          {props.body.map((b, i) => (
            <BodyShort key={i} spacing>
              {b}
            </BodyShort>
          ))}
        </Alert>
        <Box padding="4" className="flex justify-center gap-[1rem]">
          <Button
            onClick={() => {
              props.navigate("/");
            }}
            variant="primary"
          >
            Gå til forsiden
          </Button>
        </Box>
      </Box>
    </Page>
  );
}
