import React from "react";
import { InformationSquareIcon } from "@navikt/aksel-icons";
import { Card, Text } from "@sanity/ui";

interface Props {
  melding?: string;
}

export const Information = ({
  melding = "Ikke del personopplysninger i fritekstfeltene",
}: Props) => {
  return (
    <Card padding={[3, 3, 4]} radius={2} shadow={1} tone="primary">
      <Text size={3}>
        {" "}
        <InformationSquareIcon width="20px" height="auto" style={{ marginRight: "0.5rem" }} />
        {melding}
      </Text>
    </Card>
  );
};
