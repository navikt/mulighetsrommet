import { List } from "@navikt/ds-react";
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
    bullet: ({ children }) => <List as="ul">{children}</List>,
    number: ({ children }) => <List as="ol">{children}</List>,
  },
  listItem: {
    bullet: ({ children }) => <List.Item>{children}</List.Item>,
    number: ({ children }) => <List.Item>{children}</List.Item>,
  },
};
