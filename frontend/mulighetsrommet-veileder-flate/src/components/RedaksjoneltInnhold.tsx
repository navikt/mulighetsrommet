import { PortableText, PortableTextBlock, PortableTextReactComponents } from "@portabletext/react";
import { Alert, BodyLong, GuidePanel, List } from "@navikt/ds-react";
import { PortableTextTypedObject } from "@api-client";

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
    normal: ({ children }) => (
      <BodyLong size="small" className="mb-1 min-h-[0.75rem]">
        {children}
      </BodyLong>
    ),
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
  const convertedValue = convertSlateToPortableText(value) as unknown as PortableTextBlock[];
  return <PortableText value={convertedValue} components={portableTextComponent} />;
}

function getOrAddKey(obj: { _key?: string | null }): string {
  return obj._key ?? crypto.randomUUID().slice(0, 8);
}

function convertSlateBlock(block: PortableTextTypedObject): PortableTextTypedObject {
  // eslint-disable-next-line @typescript-eslint/no-unnecessary-condition
  if (block._key !== null) {
    // not from slate
    return block;
  }

  const newBlock: PortableTextTypedObject = { ...block };
  if (block._type === "block") {
    newBlock["style"] = "normal";
    // required to display anything
    newBlock._key = getOrAddKey(newBlock);
    if ("listItem" in newBlock) {
      // indent existing lists
      newBlock["level"] = 1;
    }
    // Fix existing links
    if ("markDefs" in newBlock && Array.isArray(newBlock.markDefs)) {
      newBlock.markDefs = newBlock["markDefs"] ?? [];
      const linkMarkDefIndex = ((newBlock.markDefs || []) as PortableTextTypedObject[]).findIndex(
        (obj) => obj._type === "link",
      );
      if (linkMarkDefIndex > -1) {
        const linkMarkDef = (newBlock.markDefs as PortableTextTypedObject[])[linkMarkDefIndex];
        const newKey = getOrAddKey({ _key: null });
        newBlock.children = (newBlock.children as PortableTextTypedObject[]).map((child) => {
          if ("marks" in child) {
            child.marks = (child.marks as string[]).map((mark) => {
              if (mark === linkMarkDef._key) {
                return newKey;
              }
              return mark;
            });
          }
          child._key = getOrAddKey(child);
          return child;
        });
        (newBlock.markDefs as PortableTextTypedObject[])[linkMarkDefIndex] = {
          ...linkMarkDef,
          _key: newKey,
        };
      }
    }
  }
  return newBlock;
}

// PortableText editor requires _key to not be null in certain blocks
// Only attemt to fix block if null-key is detected
function convertSlateToPortableText(
  slateData: PortableTextTypedObject[] | undefined | null,
): PortableTextTypedObject[] | null {
  if (!slateData) {
    return null;
  }
  if (!slateData.length) {
    return [];
  }
  return slateData.map(convertSlateBlock);
}
