import { BodyLong, List } from "@navikt/ds-react";
import {
  PortableText as PortableTextLib,
  PortableTextProps,
  PortableTextReactComponents,
} from "@portabletext/react";

export function PortableText({ value }: { value: PortableTextProps["value"] }) {
  return <PortableTextLib value={value} components={components} />;
}

const components: Partial<PortableTextReactComponents> = {
  list: {
    bullet: ({ children }) => (
      <List size="small" as="ul">
        {children}
      </List>
    ),
    number: ({ children }) => (
      <List size="small" as="ol">
        {children}
      </List>
    ),
  },
  listItem: {
    bullet: ({ children }) => <List.Item>{children}</List.Item>,
    number: ({ children }) => <List.Item>{children}</List.Item>,
  },
  block: {
    normal: ({ children }) => <BodyLong size="small">{children}</BodyLong>,
  },
};
