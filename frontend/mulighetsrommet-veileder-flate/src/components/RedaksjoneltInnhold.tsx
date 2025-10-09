import { PortableText, PortableTextReactComponents } from "@portabletext/react";
import { Alert, BodyLong, GuidePanel, List } from "@navikt/ds-react";

interface ImageProp {
  value: { asset: { url: string }; altText: string };
}

interface TipsProps {
  value: { innhold: Record<any, any> };
}

interface AlertMessageProps {
  value: { variant: "info" | "warning" | "error"; innhold: Record<any, any> };
}

const portableTextComponent: Partial<PortableTextReactComponents> = {
  marks: {
    link: ({ children, value }) => {
      return (
        <a href={value.href} rel="noreferrer noopener" target="_blank">
          {children}
        </a>
      );
    },
  },
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
  types: {
    image: ({ value }: ImageProp) => {
      return (
        <a href={value.asset.url}>
          <img src={value.asset.url} alt={value.altText} />
        </a>
      );
    },
    tips: ({ value }: TipsProps) => {
      return (
        <GuidePanel>
          <PortableText value={value.innhold} components={portableTextComponent} />
        </GuidePanel>
      );
    },
    alertMessage: ({ value }: AlertMessageProps) => {
      return (
        <Alert style={{ margin: "1rem 0" }} variant={value.variant}>
          <PortableText value={value.innhold} components={portableTextComponent} />
        </Alert>
      );
    },
  },
};

interface RedaksjoneltinnholdProps {
  value: any;
}

export function RedaksjoneltInnhold({ value }: RedaksjoneltinnholdProps) {
  return <PortableText value={value} components={portableTextComponent} />;
}
