import { BodyLong, List, Box } from "@navikt/ds-react";
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
      <Box marginBlock="space-12" asChild>
        <List data-aksel-migrated-v8 size="small" as="ul">
          {children}
        </List>
      </Box>
    ),
    number: ({ children }) => (
      <Box marginBlock="space-12" asChild>
        <List data-aksel-migrated-v8 size="small" as="ol">
          {children}
        </List>
      </Box>
    ),
  },
  listItem: {
    bullet: ({ children }) => <List.Item>{children}</List.Item>,
    number: ({ children }) => <List.Item>{children}</List.Item>,
  },
  block: {
    normal: ({ children }) => (
      <BodyLong size="small" className="mb-1 min-h-[0.75rem]">
        {children}
      </BodyLong>
    ),
  },
};
